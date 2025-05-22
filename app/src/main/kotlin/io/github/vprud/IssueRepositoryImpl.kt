package io.github.vprud

import io.github.vprud.table.IssueTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

interface IssueRepository {
    fun save(issue: GitHubIssue)

    fun exists(
        issueNumber: Int,
        repository: String,
    ): Boolean
}

class IssueRepositoryImpl : IssueRepository {
    override fun save(issue: GitHubIssue) =
        transaction {
            if (!exists(issue.number, getRepoFromUrl(issue.repositoryUrl))) {
                IssueTable.insert {
                    it[issueNumber] = issue.number
                    it[repository] = getRepoFromUrl(issue.repositoryUrl)
                    it[title] = issue.title
                    it[body] = issue.body ?: ""
                    it[labels] = issue.labels.map { label -> label.name }
                    it[htmlUrl] = issue.htmlUrl
                    it[state] = issue.state
                    it[createdAt] = Instant.parse(issue.createdAt)
                    it[updatedAt] = Instant.parse(issue.updatedAt)
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
