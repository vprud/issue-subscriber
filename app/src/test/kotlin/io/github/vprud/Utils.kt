package io.github.vprud

import io.github.vprud.GitHubIssue.Label
import io.github.vprud.GitHubIssue.Milestone

fun testIssue(
    number: Int = 1,
    title: String = "Test Issue",
    labels: List<Label> = emptyList(),
    milestone: Milestone? = null,
) = GitHubIssue(
    url = "https://api.github.com/issues/$number",
    repositoryUrl = "https://api.github.com/repos/owner/repo",
    htmlUrl = "https://github.com/owner/repo/issues/$number",
    number = number,
    title = title,
    labels = labels,
    state = "open",
    milestone = milestone,
    comments = 0,
    createdAt = "2023-01-01T00:00:00Z",
    updatedAt = "2023-01-01T00:00:00Z",
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
