package io.github.vprud

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class NotificationServiceTest {
    private val issueUpdateService = mockk<IssueUpdateService>()
    private val service = NotificationService(issueUpdateService)

    @Test
    fun `checkAndNotify should call notify for each update`() {
        val issue =
            testIssue(
                number = 1,
                title = "Issue 1",
                labels = emptyList(),
            )

        every { issueUpdateService.checkForUpdates() } returns
            mapOf(
                1L to listOf(issue),
                2L to listOf(issue, issue),
            )

        val notifier = mockk<(Long, GitHubIssue) -> Unit>(relaxed = true)

        service.checkAndNotify(notifier) { _, _ -> }

        verify(exactly = 1) { notifier(1, issue) }
        verify(exactly = 2) { notifier(2, issue) }
    }
}
