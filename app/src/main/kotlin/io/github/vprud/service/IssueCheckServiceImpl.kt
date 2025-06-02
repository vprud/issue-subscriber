package io.github.vprud.service

import io.github.vprud.domain.Issue
import io.github.vprud.domain.Subscription
import io.github.vprud.provider.database.IssueRepository
import io.github.vprud.provider.database.SubscriptionRepository
import io.github.vprud.provider.github.GitHubClient

class IssueCheckServiceImpl(
    private val gitHubClient: GitHubClient,
    private val subscriptionRepository: SubscriptionRepository,
    private val issueRepository: IssueRepository,
) : IssueCheckService {
    override fun checkForUpdates(): Map<Long, List<Issue>> {
        val updates = mutableMapOf<Long, MutableList<Issue>>()
        val subscriptions = subscriptionRepository.getAll()
        val subscriptionsByRepo = subscriptions.groupBy { it.repository }

        subscriptionsByRepo.keys.forEach { repo ->
            try {
                val subsForRepo = subscriptionsByRepo[repo] ?: emptyList()
                val newIssues =
                    gitHubClient.fetchNewIssues(repo)

                newIssues.forEach { issue ->
                    if (!issueRepository.exists(issue.number, repo)) {
                        issueRepository.save(issue)
                    }

                    subsForRepo.forEach { sub ->
                        if (isIssueRelevant(issue, sub)) {
                            updates.getOrPut(sub.chatId) { mutableListOf() }.add(issue)
                            subscriptionRepository.updateLastChecked(sub.chatId, repo, issue.number)
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error checking updates for $repo: ${e.message}")
            }
        }

        return updates
    }

    private fun isIssueRelevant(
        issue: Issue,
        sub: Subscription,
    ): Boolean =
        (
            sub.labels.isEmpty() ||
                issue.labels.any { label -> sub.labels.contains(label) }
        ) &&
            issue.number > (sub.lastCheckedIssueId ?: 0)
}
