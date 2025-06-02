package io.github.vprud.provider.database

import io.github.vprud.domain.Subscription

interface SubscriptionRepository {
    fun add(subscription: Subscription): Int

    fun remove(
        chatId: Long,
        repository: String,
    ): Boolean

    fun getAll(): List<Subscription>

    fun getByChatId(chatId: Long): List<Subscription>

    fun get(
        chatId: Long,
        repository: String,
    ): Subscription?

    fun updateLastChecked(
        chatId: Long,
        repository: String,
        lastIssueId: Int,
    ): Int
}
