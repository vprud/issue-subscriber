package io.github.vprud.provider.database

import io.github.vprud.databse.table.IssueTable
import io.github.vprud.domain.Issue
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class IssueRepositoryImpl : IssueRepository {
    private fun ResultRow.toIssue(): Issue =
        Issue(
            number = this[IssueTable.issueNumber],
            repositoryUrl = this[IssueTable.repository],
            title = this[IssueTable.title],
            labels = this[IssueTable.labels],
            body = this[IssueTable.body],
            htmlUrl = this[IssueTable.htmlUrl],
            state = this[IssueTable.state],
            createdAt = this[IssueTable.createdAt],
            updatedAt = this[IssueTable.updatedAt],
        )

    override fun save(issue: Issue) =
        transaction {
            if (!exists(issue.number, getRepoFromUrl(issue.repositoryUrl))) {
                IssueTable.insert {
                    it[issueNumber] = issue.number
                    it[repository] = getRepoFromUrl(issue.repositoryUrl)
                    it[title] = issue.title
                    it[body] = issue.body ?: ""
                    it[labels] = issue.labels
                    it[htmlUrl] = issue.htmlUrl
                    it[state] = issue.state
                    it[createdAt] = issue.createdAt
                    it[updatedAt] = issue.updatedAt
                }
            }
        }

    override fun exists(
        issueNumber: Int,
        repository: String,
    ) = transaction {
        IssueTable
            .selectAll()
            .where {
                (IssueTable.issueNumber eq issueNumber) and (IssueTable.repository eq repository)
            }.count() > 0
    }

    private fun getRepoFromUrl(url: String): String = url.removePrefix("https://api.github.com/repos/")
}
