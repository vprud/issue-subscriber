package io.github.vprud

import io.mockk.*
import org.junit.jupiter.api.Disabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IssueUpdateServiceTest {
    private val gitHubClient = mockk<GitHubClient>()
    private val subscriptionManager = mockk<SubscriptionManager>()
    private val issueRepository = mockk<IssueRepository>()
    private val service = IssueUpdateService(gitHubClient, subscriptionManager, issueRepository)

    @Disabled("Need fix test itself")
    @Test
    fun `checkForUpdates should return filtered issues by labels`() {
        val issue1 =
            testIssue(
                number = 1,
                title = "Issue 1",
                labels = listOf(GitHubIssue.Label(1, "url", "bug", "red")),
            )
        val issue2 =
            testIssue(
                number = 2,
                title = "Issue 2",
                labels = listOf(GitHubIssue.Label(2, "url", "feature", "blue")),
            )

        every { subscriptionManager.getAllSubscriptions() } returns
            listOf(
                Subscription(1, "repo1", setOf("bug")),
                Subscription(2, "repo1", setOf("feature")),
            )
        every { gitHubClient.fetchNewIssues("repo1", any()) } returns listOf(issue1, issue2)
        every { subscriptionManager.updateLastChecked(any(), any(), any()) } just Awaits

        val updates = service.checkForUpdates()

        assertEquals(1, updates[1]?.size)
        assertEquals("Issue 1", updates[1]?.get(0)?.title)
        assertEquals(1, updates[2]?.size)
        assertEquals("Issue 2", updates[2]?.get(0)?.title)

        /*verify {
            subscriptionManager.updateLastChecked(1, "repo1", 1)
            subscriptionManager.updateLastChecked(2, "repo1", 2)
        }*/
    }

    @Disabled("Need fix test itself")
    @Test
    fun `checkForUpdates should return filtered issues by last checked issue id`() {
        val issue1 =
            testIssue(
                number = 1,
                title = "Issue 1",
                labels = listOf(),
            )
        val issue2 =
            testIssue(
                number = 2,
                title = "Issue 2",
                labels = listOf(),
            )

        every { subscriptionManager.getAllSubscriptions() } returns
            listOf(
                Subscription(1, "repo1", lastCheckedIssueId = 1),
                Subscription(2, "repo1", lastCheckedIssueId = 2),
            )
        every { gitHubClient.fetchNewIssues("repo1", any()) } returns listOf(issue1, issue2)
        every { subscriptionManager.updateLastChecked(any(), any(), any()) } just Awaits

        val updates = service.checkForUpdates()

        assertEquals(1, updates[1]?.size)
        assertEquals("Issue 2", updates[1]?.get(0)?.title)
        assertEquals(null, updates[2]?.size)

        verify {
            subscriptionManager.updateLastChecked(1, "repo1", 2)
        }
    }

    @Test
    fun `checkForUpdates should handle exceptions gracefully`() {
        every { subscriptionManager.getAllSubscriptions() } returns
            listOf(
                Subscription(1, "repo1"),
            )
        every { gitHubClient.fetchNewIssues("repo1", any()) } throws RuntimeException("API error")

        val updates = service.checkForUpdates()

        assertTrue(updates.isEmpty())
    }
}
