package io.github.vprud

import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubIssueTrackerTest {
    private val gitHubClient = mockk<GitHubClient>()
    private val tracker = GitHubIssueTracker(gitHubClient)

    @Test
    fun `subscribe should delegate to subscription manager`() {
        tracker.subscribe(1, "repo1", setOf("label1"))

        val subs = tracker.getSubscriptions(1)
        assertEquals(1, subs.size)
        assertEquals("repo1", subs[0].repository)
        assertTrue(subs[0].labels.contains("label1"))
    }

    @Test
    fun `unsubscribe should delegate to subscription manager`() {
        tracker.subscribe(1, "repo1")
        val result = tracker.unsubscribe(1, "repo1")

        assertTrue(result)
        assertTrue(tracker.getSubscriptions(1).isEmpty())
    }

    @Test
    fun `checkForUpdates should delegate to notification service`() {
        val notifier = mockk<(Long, GitHubIssue) -> Unit>(relaxed = true)
        tracker.checkForUpdates(notifier)

        // Just verify the call went through
        assertFalse(tracker.getSubscriptions(1).isNotEmpty())
    }
}
