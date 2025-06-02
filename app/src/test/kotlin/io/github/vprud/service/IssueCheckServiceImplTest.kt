package io.github.vprud.service

import io.github.vprud.ExtensionDatabase
import io.github.vprud.domain.Subscription
import io.github.vprud.provider.database.IssueRepositoryImpl
import io.github.vprud.provider.database.SubscriptionRepositoryImpl
import io.github.vprud.provider.github.GitHubClientImpl
import io.github.vprud.testIssue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(ExtensionDatabase::class)
class IssueCheckServiceImplTest {
    private val gitHubClient = mockk<GitHubClientImpl>()
    private val subscriptionRepository = SubscriptionRepositoryImpl()
    private val issueRepository = IssueRepositoryImpl()
    private val service = IssueCheckServiceImpl(gitHubClient, subscriptionRepository, issueRepository)

    @Test
    fun `checkForUpdates should handle exceptions gracefully`() {
        // Given
        val chatId = 123L
        val repo = "owner/repo"
        subscriptionRepository.add(Subscription(chatId, repo))

        every { gitHubClient.fetchNewIssues(repo) } throws RuntimeException("API error")

        // When
        val updates = service.checkForUpdates()

        // Then
        assertTrue(updates.isEmpty())
    }

    @Test
    fun `checkForUpdates should handle empty subscriptions`() {
        // When
        val updates = service.checkForUpdates()

        // Then
        assertTrue(updates.isEmpty())
    }

    @Test
    fun `checkForUpdates should return new issues for subscribers`() {
        // Given
        val chatId = 123L
        val repo = "owner/repo"
        subscriptionRepository.add(Subscription(chatId, repo))

        val issue =
            testIssue(
                number = 1,
                title = "New Issue",
                labels = listOf("bug"),
            )

        every { gitHubClient.fetchNewIssues(repo) } returns listOf(issue)

        // When
        val updates = service.checkForUpdates()

        // Then
        assertEquals(1, updates.size)
        assertEquals(listOf(issue), updates[chatId])
    }

    @Test
    fun `checkForUpdates should filter issues by subscription labels`() {
        // Given
        val chatId = 123L
        val repo = "owner/repo"
        subscriptionRepository.add(Subscription(chatId, repo, labels = setOf("bug")))

        val bugIssue =
            testIssue(
                number = 1,
                title = "Bug Issue",
                labels = listOf("bug"),
            )
        val featureIssue =
            testIssue(
                number = 2,
                title = "Feature Issue",
                labels = listOf("feature"),
            )

        every { gitHubClient.fetchNewIssues(repo) } returns listOf(bugIssue, featureIssue)

        // When
        val updates = service.checkForUpdates()

        // Then
        assertEquals(1, updates.size)
        assertEquals(listOf(bugIssue), updates[chatId])
    }

    @Test
    fun `checkForUpdates should handle multiple subscriptions`() {
        // Given
        val chatId1 = 123L
        val chatId2 = 456L
        val repo1 = "owner1/repo1"
        val repo2 = "owner2/repo2"
        subscriptionRepository.add(Subscription(chatId1, repo1))
        subscriptionRepository.add(Subscription(chatId2, repo2))

        val issue1 =
            testIssue(
                number = 1,
                title = "Issue 1",
                labels = listOf("bug"),
            )
        val issue2 =
            testIssue(
                number = 2,
                title = "Issue 2",
                labels = listOf("feature"),
            )

        every { gitHubClient.fetchNewIssues(repo1) } returns listOf(issue1)
        every { gitHubClient.fetchNewIssues(repo2) } returns listOf(issue2)

        // When
        val updates = service.checkForUpdates()

        // Then
        assertEquals(2, updates.size)
        assertEquals(listOf(issue1), updates[chatId1])
        assertEquals(listOf(issue2), updates[chatId2])
    }

    @Test
    fun `checkForUpdates should skip already checked issues`() {
        // Given
        val chatId = 123L
        val repo = "owner/repo"
        subscriptionRepository.add(Subscription(chatId, repo))

        val oldIssue =
            testIssue(
                number = 1,
                title = "Old Issue",
                labels = listOf("bug"),
            )

        subscriptionRepository.updateLastChecked(chatId, repo, 1)
        every { gitHubClient.fetchNewIssues(repo) } returns listOf(oldIssue)

        // When
        val updates = service.checkForUpdates()

        // Then
        assertEquals(0, updates.size)
    }
}
