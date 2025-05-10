package io.github.vprud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionManagerTest {
    private val manager = SubscriptionManager()

    @Test
    fun `should add new subscription`() {
        manager.addSubscription(1, "repo1")

        val subs = manager.getUserSubscriptions(1)
        assertEquals(1, subs.size)
        assertEquals("repo1", subs[0].repository)
    }

    @Test
    fun `should overwrite existing subscription`() {
        manager.addSubscription(1, "repo1", setOf("label1"))
        manager.addSubscription(1, "repo1", setOf("label2"))

        val subs = manager.getUserSubscriptions(1)
        assertEquals(1, subs.size)
        assertTrue(subs[0].labels.contains("label2"))
    }

    @Test
    fun `should remove existing subscription`() {
        manager.addSubscription(1, "repo1")
        val result = manager.removeSubscription(1, "repo1")

        assertTrue(result)
        assertTrue(manager.getUserSubscriptions(1).isEmpty())
    }

    @Test
    fun `should return false when subscription not exists`() {
        val result = manager.removeSubscription(1, "repo1")

        assertFalse(result)
    }

    @Test
    fun `updateLastChecked should update correct subscription`() {
        manager.addSubscription(1, "repo1")
        manager.addSubscription(1, "repo2")

        manager.updateLastChecked(1, "repo1", 100)

        assertEquals(100, manager.getSubscription(1, "repo1")?.lastCheckedIssueId)
        assertNull(manager.getSubscription(1, "repo2")?.lastCheckedIssueId)
    }

    @Test
    fun `getAllSubscriptions should return all subscriptions`() {
        manager.addSubscription(1, "repo1")
        manager.addSubscription(2, "repo2")

        val allSubs = manager.getAllSubscriptions()

        assertEquals(2, allSubs.size)
        assertTrue(allSubs.any { it.repository == "repo1" })
        assertTrue(allSubs.any { it.repository == "repo2" })
    }
}
