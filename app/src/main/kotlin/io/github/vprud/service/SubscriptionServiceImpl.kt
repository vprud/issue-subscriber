package io.github.vprud.service

import io.github.vprud.domain.Subscription
import io.github.vprud.provider.database.SubscriptionRepository

class SubscriptionServiceImpl(
    private val subscriptionRepository: SubscriptionRepository,
) : SubscriptionService {
    override fun subscribe(
        chatId: Long,
        repository: String,
        labels: Set<String>,
    ) {
        subscriptionRepository.add(Subscription(chatId, repository, labels))
    }

    override fun unsubscribe(
        chatId: Long,
        repository: String,
    ): Boolean = subscriptionRepository.remove(chatId, repository)

    override fun getSubscriptions(chatId: Long): List<Subscription> = subscriptionRepository.getByChatId(chatId)
}
