package io.github.vprud

import org.jetbrains.exposed.sql.transactions.transaction

data class Subscription(
    val chatId: Long,
    val repository: String,
    val labels: Set<String> = emptySet(),
    var lastCheckedIssueId: Int? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Subscription

        if (chatId != other.chatId) return false
        if (repository != other.repository) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chatId.hashCode()
        result = 31 * result + repository.hashCode()
        return result
    }
}

class SubscriptionManager(
    private val subscriptionRepository: SubscriptionRepository,
) {
    fun addSubscription(
        chatId: Long,
        repository: String,
        labels: Set<String> = emptySet(),
    ) = transaction { subscriptionRepository.add(Subscription(chatId, repository, labels)) }

    fun removeSubscription(
        chatId: Long,
        repository: String,
    ) = transaction { subscriptionRepository.remove(chatId, repository) }

    fun getUserSubscriptions(chatId: Long): List<Subscription> = transaction { subscriptionRepository.getByChatId(chatId) }

    fun getAllSubscriptions(): List<Subscription> = transaction { subscriptionRepository.getAll() }

    fun updateLastChecked(
        chatId: Long,
        repository: String,
        lastIssueId: Int,
    ) = transaction { subscriptionRepository.updateLastChecked(chatId, repository, lastIssueId) }

    fun getSubscription(
        chatId: Long,
        repository: String,
    ) = transaction { subscriptionRepository.get(chatId, repository) }

    fun hasSubscription(
        chatId: Long,
        repository: String,
    ) = transaction { subscriptionRepository.get(chatId, repository) != null }
}

class IssueUpdateService(
    private val gitHubClient: GitHubClient,
    private val subscriptionManager: SubscriptionManager,
) {
    fun checkForUpdates(): Map<Long, List<GitHubIssue>> {
        val updates = mutableMapOf<Long, MutableList<GitHubIssue>>()

        val subscriptionsByRepo =
            subscriptionManager
                .getAllSubscriptions()
                .groupBy { it.repository }

        subscriptionsByRepo.forEach { (repo, subs) ->
            try {
                val lastCheckedId = subs.maxOfOrNull { it.lastCheckedIssueId ?: 0 } ?: 0

                val newIssues = gitHubClient.fetchNewIssues(repo, lastCheckedId)

                subs.forEach { sub ->
                    val filteredIssues =
                        newIssues.filter { issue ->
                            // Если labels не указаны, показываем все issues
                            (
                                sub.labels.isEmpty() ||
                                    issue.labels.any { label ->
                                        sub.labels.contains(label.name)
                                    }
                            ) &&
                                issue.number > (sub.lastCheckedIssueId ?: 0)
                        }

                    if (filteredIssues.isNotEmpty()) {
                        updates.getOrPut(sub.chatId) { mutableListOf() }.addAll(filteredIssues)
                        // Обновляем lastCheckedIssueId для подписки
                        val maxIssueId = filteredIssues.maxOf { it.number }
                        subscriptionManager.updateLastChecked(sub.chatId, repo, maxIssueId)
                    }
                }
            } catch (e: Exception) {
                // Логируем ошибку и продолжаем проверку других репозиториев
                println("Error checking updates for $repo: ${e.message}")
            }
        }

        return updates
    }
}

class NotificationService(
    private val issueUpdateService: IssueUpdateService,
) {
    fun checkAndNotify(
        notify: (chatId: Long, issue: GitHubIssue) -> Unit,
        callback: (chatId: Long, issue: GitHubIssue) -> Unit,
    ) {
        val updates = issueUpdateService.checkForUpdates()

        updates.forEach { (chatId, issues) ->
            issues.forEach { issue ->
                notify(chatId, issue)
                callback(chatId, issue)
            }
        }
    }
}

class GitHubIssueTracker(
    private val gitHubClient: GitHubClient,
    private val subscriptionManager: SubscriptionManager,
    private val notificationService: NotificationService =
        IssueUpdateService(gitHubClient, subscriptionManager).let {
            NotificationService(it)
        },
) {
    fun subscribe(
        chatId: Long,
        repository: String,
        labels: Set<String> = emptySet(),
    ) {
        subscriptionManager.addSubscription(chatId, repository, labels)
    }

    fun unsubscribe(
        chatId: Long,
        repository: String,
    ): Boolean = subscriptionManager.removeSubscription(chatId, repository)

    fun getSubscriptions(chatId: Long): List<Subscription> = subscriptionManager.getUserSubscriptions(chatId)

    fun checkForUpdates(notify: (chatId: Long, issue: GitHubIssue) -> Unit) {
        notificationService.checkAndNotify(notify) { chatId, issue ->
            subscriptionManager.updateLastChecked(
                chatId,
                issue.repositoryUrl,
                issue.number,
            )
        }
    }
}
