package io.github.vprud.service

import io.github.vprud.domain.Subscription
import io.github.vprud.provider.database.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SubscriptionServiceImplTest {
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var subscriptionService: SubscriptionServiceImpl

    @BeforeEach
    fun setup() {
        subscriptionRepository = mockk(relaxed = true)
        subscriptionService = SubscriptionServiceImpl(subscriptionRepository)
    }

    @Test
    fun `should add subscription`() {
        // given
        val chatId = 123L
        val repository = "https://github.com/owner/repo"
        val labels = setOf("bug", "enhancement")

        // when
        subscriptionService.subscribe(chatId, repository, labels)

        // then
        verify { subscriptionRepository.add(Subscription(chatId, repository, labels)) }
    }

    @Test
    fun `should remove subscription and return true when subscription exists`() {
        // given
        val chatId = 123L
        val repository = "https://github.com/owner/repo"
        every { subscriptionRepository.remove(chatId, repository) } returns true

        // when
        val result = subscriptionService.unsubscribe(chatId, repository)

        // then
        assertTrue(result)
        verify { subscriptionRepository.remove(chatId, repository) }
    }

    @Test
    fun `should return false when subscription does not exist`() {
        // given
        val chatId = 123L
        val repository = "https://github.com/owner/repo"
        every { subscriptionRepository.remove(chatId, repository) } returns false

        // when
        val result = subscriptionService.unsubscribe(chatId, repository)

        // then
        assertFalse(result)
        verify { subscriptionRepository.remove(chatId, repository) }
    }

    @Test
    fun `should get subscriptions for chat ID`() {
        // given
        val chatId = 123L
        val subscriptions =
            listOf(
                Subscription(chatId, "https://github.com/owner/repo1", setOf("bug")),
                Subscription(chatId, "https://github.com/owner/repo2", setOf("enhancement")),
            )
        every { subscriptionRepository.getByChatId(chatId) } returns subscriptions

        // when
        val result = subscriptionService.getSubscriptions(chatId)

        // then
        assertEquals(subscriptions, result)
        verify { subscriptionRepository.getByChatId(chatId) }
    }

    @Test
    fun `should return empty list when no subscriptions exist`() {
        // given
        val chatId = 123L
        every { subscriptionRepository.getByChatId(chatId) } returns emptyList()

        // when
        val result = subscriptionService.getSubscriptions(chatId)

        // then
        assertTrue(result.isEmpty())
        verify { subscriptionRepository.getByChatId(chatId) }
    }
}
