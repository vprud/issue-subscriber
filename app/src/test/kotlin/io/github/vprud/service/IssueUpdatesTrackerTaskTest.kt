package io.github.vprud.service

import io.github.vprud.domain.Issue
import io.github.vprud.provider.database.SubscriptionRepository
import io.github.vprud.task.Notify
import io.github.vprud.testIssue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IssueUpdatesTrackerTaskTest {
    @Test
    fun `should notify about updates and update last checked timestamp`() {
        // Given
        val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
        val issueCheckService = mockk<IssueCheckService>()
        val task = IssueUpdatesTrackerTask(subscriptionRepository, issueCheckService)

        val chatId = 123L
        val issue =
            testIssue(
                number = 1,
                title = "Test Issue",
                labels = listOf("bug"),
            )

        val updates = mapOf(chatId to listOf(issue))
        every { issueCheckService.checkForUpdates() } returns updates

        var notifiedChatId: Long? = null
        var notifiedIssue: Issue? = null
        val notify: Notify = { _, _ ->
            notifiedChatId = chatId
            notifiedIssue = issue
        }

        // When
        task.run(notify)

        // Then
        assertEquals(chatId, notifiedChatId)
        assertEquals(issue, notifiedIssue)
        verify { subscriptionRepository.updateLastChecked(chatId, issue.repositoryUrl, issue.number) }
    }

    @Test
    fun `should handle multiple updates for different chats`() {
        // Given
        val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
        val issueCheckService = mockk<IssueCheckService>()
        val task = IssueUpdatesTrackerTask(subscriptionRepository, issueCheckService)

        val chatId1 = 123L
        val chatId2 = 456L
        val issue1 =
            testIssue(
                number = 1,
                title = "Issue 1",
                labels = listOf("bug"),
            )
        val issue2 =
            testIssue(
                number = 2,
                title = "Issue 2",
                labels = listOf("enhancement"),
            )

        val updates =
            mapOf(
                chatId1 to listOf(issue1),
                chatId2 to listOf(issue2),
            )
        every { issueCheckService.checkForUpdates() } returns updates

        val notifiedUpdates = mutableMapOf<Long, Issue>()
        val notify: Notify = { chatId, issue ->
            notifiedUpdates[chatId] = issue
        }

        // When
        task.run(notify)

        // Then
        assertEquals(2, notifiedUpdates.size)
        assertEquals(issue1, notifiedUpdates[chatId1])
        assertEquals(issue2, notifiedUpdates[chatId2])
        verify { subscriptionRepository.updateLastChecked(chatId1, issue1.repositoryUrl, issue1.number) }
        verify { subscriptionRepository.updateLastChecked(chatId2, issue2.repositoryUrl, issue2.number) }
    }

    @Test
    fun `should handle empty updates`() {
        // Given
        val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
        val issueCheckService = mockk<IssueCheckService>()
        val task = IssueUpdatesTrackerTask(subscriptionRepository, issueCheckService)

        every { issueCheckService.checkForUpdates() } returns emptyMap()

        var notificationCalled = false
        val notify: Notify = { _, _ ->
            notificationCalled = true
        }

        // When
        task.run(notify)

        // Then
        assertEquals(false, notificationCalled)
    }

    @Test
    fun `should handle multiple issues for single chat`() {
        // Given
        val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
        val issueCheckService = mockk<IssueCheckService>()
        val task = IssueUpdatesTrackerTask(subscriptionRepository, issueCheckService)

        val chatId = 123L
        val issue1 =
            testIssue(
                number = 1,
                title = "Issue 1",
                labels = listOf("bug"),
            )
        val issue2 =
            testIssue(
                number = 2,
                title = "Issue 2",
                labels = listOf("enhancement"),
            )

        val updates = mapOf(chatId to listOf(issue1, issue2))
        every { issueCheckService.checkForUpdates() } returns updates

        val notifiedIssues = mutableListOf<Issue>()
        val notify: Notify = { _, issue ->
            notifiedIssues.add(issue)
        }

        // When
        task.run(notify)

        // Then
        assertEquals(2, notifiedIssues.size)
        assertEquals(issue1, notifiedIssues[0])
        assertEquals(issue2, notifiedIssues[1])
        verify { subscriptionRepository.updateLastChecked(chatId, issue1.repositoryUrl, issue1.number) }
        verify { subscriptionRepository.updateLastChecked(chatId, issue2.repositoryUrl, issue2.number) }
    }

    @Test
    fun `should handle issues without labels`() {
        // Given
        val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
        val issueCheckService = mockk<IssueCheckService>()
        val task = IssueUpdatesTrackerTask(subscriptionRepository, issueCheckService)

        val chatId = 123L
        val issue =
            testIssue(
                number = 1,
                title = "Issue without labels",
                labels = emptyList(),
            )

        val updates = mapOf(chatId to listOf(issue))
        every { issueCheckService.checkForUpdates() } returns updates

        var notifiedIssue: Issue? = null
        val notify: Notify = { _, issue ->
            notifiedIssue = issue
        }

        // When
        task.run(notify)

        // Then
        assertEquals(issue, notifiedIssue)
        verify { subscriptionRepository.updateLastChecked(chatId, issue.repositoryUrl, issue.number) }
    }
}
