package io.github.vprud.service

import io.github.vprud.domain.Subscription

interface SubscriptionService {
    fun subscribe(
        chatId: Long,
        repository: String,
        labels: Set<String> = emptySet(),
    )

    fun unsubscribe(
        chatId: Long,
        repository: String,
    ): Boolean

    fun getSubscriptions(chatId: Long): List<Subscription>
}
