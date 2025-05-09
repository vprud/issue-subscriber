package io.github.vprud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubscriptionTest {
    @Test
    fun `subscription equality should work correctly`() {
        val sub1 = Subscription(1, "repo1", setOf("label1"))
        val sub2 = Subscription(1, "repo1", setOf("label2"))
        val sub3 = Subscription(2, "repo1", setOf("label1"))

        assertEquals(sub1, sub2)
        assertTrue(sub1 != sub3)
        assertEquals(sub1.hashCode(), sub2.hashCode())
    }

    @Test
    fun `subscription should correctly initialize`() {
        val sub = Subscription(1, "repo1", setOf("label1"), 100)

        assertEquals(1, sub.chatId)
        assertEquals("repo1", sub.repository)
        assertTrue(sub.labels.contains("label1"))
        assertEquals(100, sub.lastCheckedIssueId)
    }
}
