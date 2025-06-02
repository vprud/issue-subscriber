package io.github.vprud.provider.github

import io.github.vprud.domain.Issue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubIssue(
    val url: String,
    @SerialName("repository_url")
    val repositoryUrl: String,
    @SerialName("html_url")
    val htmlUrl: String,
    val number: Int,
    val title: String,
    val labels: List<Label>,
    val state: String,
    val milestone: Milestone? = null,
    val comments: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val body: String? = null,
) {
    @Serializable
    data class Label(
        val id: Long,
        val url: String,
        val name: String,
        val color: String,
        val description: String? = null,
    )

    @Serializable
    data class Milestone(
        val url: String,
        @SerialName("html_url")
        val htmlUrl1: String,
        val id: Long,
        val title: String,
        val description: String? = null,
    )

    fun toIssue(): Issue =
        Issue(
            number = this.number,
            title = this.title,
            body = this.body ?: "",
            labels = this.labels.map { it.name },
            repositoryUrl = this.repositoryUrl,
            state = this.state,
            htmlUrl = this.htmlUrl,
            createdAt = java.time.Instant.parse(this.createdAt),
            updatedAt = java.time.Instant.parse(this.updatedAt),
        )
}
