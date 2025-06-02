package io.github.vprud.provider.github

import io.github.vprud.domain.Issue
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.Duration.Companion.hours

class GitHubClientImpl(
    private val httpClient: OkHttpClient,
    private val githubToken: String,
) : GitHubClient {
    private val apiVersion = "2022-11-28"
    private val json = Json { ignoreUnknownKeys = true }

    override fun fetchNewIssues(repository: String): List<Issue> {
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

        return json
            .decodeFromString<List<GitHubIssue>>(response.body?.string() ?: "[]")
            .map { it.toIssue() }
    }

    override fun fetchLatestIssue(repository: String): Issue? {
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

        return json
            .decodeFromString<List<GitHubIssue>>(response.body?.string() ?: "[]")
            .firstOrNull()
            .let { it?.toIssue() }
    }
}
