package io.github.vprud

import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.Duration.Companion.hours

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
    val milestone: Milestone? = null, // Может отсутствовать
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
        val description: String? = null, // Может быть null
    )

    @Serializable
    data class Milestone(
        val url: String,
        @SerialName("html_url")
        val htmlUrl1: String,
        val id: Long,
        val title: String,
        val description: String? = null, // Может быть null
    )
}

class GitHubClient(
    private val httpClient: OkHttpClient,
    private val githubToken: String,
) {
    private val apiVersion = "2022-11-28"
    private val json = Json { ignoreUnknownKeys = true }

    fun fetchNewIssues(
        repository: String,
        lastCheckedIssueId: Int?,
    ): List<GitHubIssue> {
        val now =
            Clock.System
                .now()
                .minus(1.hours)
                .toString()
        val url = "https://api.github.com/repos/$repository/issues?state=all&sort=created&direction=desc&since=$now"
        val request =
            Request
                .Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("Authorization", "Bearer $githubToken")
                .addHeader("X-GitHub-Api-Version", apiVersion)
                .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to fetch issues: ${response.code} ${response.message}")
        }

        return json.decodeFromString<List<GitHubIssue>>(response.body?.string() ?: "[]")
    }

    fun fetchLatestIssue(repository: String): GitHubIssue? {
        val url = "https://api.github.com/repos/$repository/issues?state=all&sort=created&direction=desc&per_page=1"
        val request =
            Request
                .Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("Authorization", "Bearer $githubToken")
                .addHeader("X-GitHub-Api-Version", apiVersion)
                .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to fetch latest issue: ${response.code} ${response.message}")
        }

        return json.decodeFromString<List<GitHubIssue>>(response.body?.string() ?: "[]").firstOrNull()
    }
}
