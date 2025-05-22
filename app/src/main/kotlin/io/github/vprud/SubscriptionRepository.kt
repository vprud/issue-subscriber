package io.github.vprud

import io.github.vprud.table.SubscriptionTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

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

class SubscriptionRepositoryImpl : SubscriptionRepository {
    private fun ResultRow.toSubscription(): Subscription =
        Subscription(
            chatId = this[SubscriptionTable.chatId],
            repository = this[SubscriptionTable.repository],
            labels = this[SubscriptionTable.labels].toSet(),
            lastCheckedIssueId = this[SubscriptionTable.lastCheckedIssueId],
        )

    override fun add(subscription: Subscription): Int {
        val stmt =
            SubscriptionTable.insert {
                it[chatId] = subscription.chatId
                it[repository] = subscription.repository
                it[labels] = subscription.labels.toList()
                it[lastCheckedIssueId] = subscription.lastCheckedIssueId
                it[createdAt] = java.time.Instant.now()
            }

        return stmt.insertedCount
    }

    override fun remove(
        chatId: Long,
        repository: String,
    ): Boolean =
        SubscriptionTable.deleteWhere {
            (SubscriptionTable.chatId eq chatId) and
                (SubscriptionTable.repository eq repository)
        } > 0

    override fun getAll(): List<Subscription> = SubscriptionTable.selectAll().map { it.toSubscription() }

    override fun getByChatId(chatId: Long): List<Subscription> =
        SubscriptionTable.selectAll().where { SubscriptionTable.chatId eq chatId }.map { it.toSubscription() }

    override fun get(
        chatId: Long,
        repository: String,
    ): Subscription? =
        SubscriptionTable
            .selectAll()
            .where {
                (SubscriptionTable.chatId eq chatId) and
                    (SubscriptionTable.repository eq repository)
            }.singleOrNull()
            ?.toSubscription()

    override fun updateLastChecked(
        chatId: Long,
        repository: String,
        lastIssueId: Int,
    ) = SubscriptionTable.update(
        where = { (SubscriptionTable.chatId eq chatId) and (SubscriptionTable.repository eq repository) },
    ) {
        it[lastCheckedIssueId] = lastIssueId
    }
}
