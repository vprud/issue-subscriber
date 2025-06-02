package io.github.vprud.service

import io.github.vprud.provider.database.SubscriptionRepository
import io.github.vprud.task.Notify
import io.github.vprud.task.Task

class IssueUpdatesTrackerTask(
    private val subscriptionRepository: SubscriptionRepository,
    private val issueCheckService: IssueCheckService,
) : Task {
    override fun run(notify: Notify) {
        val updates = issueCheckService.checkForUpdates()

        updates.forEach { (chatId, issues) ->
            issues.forEach { issue ->
                notify(chatId, issue)
                subscriptionRepository.updateLastChecked(
                    chatId,
                    issue.repositoryUrl,
                    issue.number,
                )
            }
        }
    }
}
