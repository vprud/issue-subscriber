package io.github.vprud.provider.database

import io.github.vprud.domain.Issue

interface IssueRepository {
    fun save(issue: Issue)

    fun exists(
        issueNumber: Int,
        repository: String,
    ): Boolean
}
