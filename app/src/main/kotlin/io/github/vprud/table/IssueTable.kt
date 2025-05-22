package io.github.vprud.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object IssueTable : Table("issue") {
    val id = integer("id").autoIncrement()
    val issueNumber = integer("issue_number")
    val repository = varchar("repository", 255)
    val title = varchar("title", 255)
    val body = text("body")
    val labels = array<String>("labels")
    val htmlUrl = varchar("html_url", 255)
    val state = varchar("state", 50)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    // Composite unique constraint
    init {
        uniqueIndex(issueNumber, repository)
    }
}
