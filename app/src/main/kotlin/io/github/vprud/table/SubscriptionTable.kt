package io.github.vprud.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object SubscriptionTable : Table("subscription") {
    val chatId = long("chat_id")
    val repository = varchar("repository", 255)
    val labels = text("labels").nullable()
    val lastCheckedIssueId = integer("last_checked_issue_id").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(chatId, repository, name = "pk_subscription")
}
