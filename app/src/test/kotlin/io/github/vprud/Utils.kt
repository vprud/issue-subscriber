package io.github.vprud

import io.github.vprud.domain.Issue
import io.github.vprud.provider.github.GitHubIssue.Label

fun testIssue(
    number: Int = 1,
    title: String = "Test Issue",
    labels: List<String> = emptyList(),
) = Issue(
    repositoryUrl = "https://api.github.com/repos/owner/repo",
    htmlUrl = "https://github.com/owner/repo/issues/$number",
    number = number,
    title = title,
    labels = labels,
    state = "open",
    createdAt = java.time.Instant.parse("2023-01-01T00:00:00Z"),
    updatedAt = java.time.Instant.parse("2023-01-01T00:00:00Z"),
    body = "Test issue body",
)

fun testLabel(
    name: String = "bug",
    color: String = "d73a4a",
) = Label(
    id = name.hashCode().toLong(),
    url = "https://api.github.com/labels/$name",
    name = name,
    color = color,
    description = "Test $name label",
)
