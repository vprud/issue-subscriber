package io.github.vprud.provider.database

import io.github.vprud.databse.table.SubscriptionTable
import io.github.vprud.domain.Subscription
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class SubscriptionRepositoryImpl : SubscriptionRepository {
    private fun ResultRow.toSubscription(): Subscription =
        Subscription(
            chatId = this[SubscriptionTable.chatId],
            repository = this[SubscriptionTable.repository],
            labels = this[SubscriptionTable.labels].toSet(),
            lastCheckedIssueId = this[SubscriptionTable.lastCheckedIssueId],
        )

    override fun add(subscription: Subscription): Int =
        transaction {
            SubscriptionTable
                .upsert {
                    it[chatId] = subscription.chatId
                    it[repository] = subscription.repository
                    it[labels] = subscription.labels.toList()
                    it[lastCheckedIssueId] = subscription.lastCheckedIssueId
                    it[createdAt] = java.time.Instant.now()
                }.insertedCount
        }

    override fun remove(
        chatId: Long,
        repository: String,
    ): Boolean =
        transaction {
            SubscriptionTable.deleteWhere {
                (SubscriptionTable.chatId eq chatId) and
                    (SubscriptionTable.repository eq repository)
            } > 0
        }

    override fun getAll(): List<Subscription> = transaction { SubscriptionTable.selectAll().map { it.toSubscription() } }

    override fun getByChatId(chatId: Long): List<Subscription> =
        transaction {
            SubscriptionTable.selectAll().where { SubscriptionTable.chatId eq chatId }.map { it.toSubscription() }
        }

    override fun get(
        chatId: Long,
        repository: String,
    ): Subscription? =
        transaction {
            SubscriptionTable
                .selectAll()
                .where {
                    (SubscriptionTable.chatId eq chatId) and
                        (SubscriptionTable.repository eq repository)
                }.singleOrNull()
                ?.toSubscription()
        }

    override fun updateLastChecked(
        chatId: Long,
        repository: String,
        lastIssueId: Int,
    ) = transaction {
        SubscriptionTable.update(
            where = { (SubscriptionTable.chatId eq chatId) and (SubscriptionTable.repository eq repository) },
        ) {
            it[lastCheckedIssueId] = lastIssueId
        }
    }
}
