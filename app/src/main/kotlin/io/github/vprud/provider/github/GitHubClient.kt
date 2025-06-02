package io.github.vprud.provider.github

import io.github.vprud.domain.Issue

interface GitHubClient {
    fun fetchNewIssues(repository: String): List<Issue>

    fun fetchLatestIssue(repository: String): Issue?
}
