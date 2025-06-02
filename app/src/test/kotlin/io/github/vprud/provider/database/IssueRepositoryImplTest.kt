package io.github.vprud.provider.database

import io.github.vprud.ExtensionDatabase
import io.github.vprud.domain.Issue
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(ExtensionDatabase::class)
class IssueRepositoryImplTest {
    private val repository = IssueRepositoryImpl()

    private fun createTestIssue(
        number: Int = 123,
        body: String = "Test issue body",
        labels: List<String> = listOf("bug"),
    ) = Issue(
        repositoryUrl = "https://api.github.com/repos/owner/repo",
        htmlUrl = "https://github.com/owner/repo/issues/123",
        number = number,
        title = "Test Issue",
        labels = labels,
        state = "open",
        createdAt = java.time.Instant.parse("2024-03-20T10:00:00Z"),
        updatedAt = java.time.Instant.parse("2024-03-20T10:00:00Z"),
        body = body,
    )

    @Test
    fun `save should insert new issue`() =
        runBlocking {
            val issue = createTestIssue()

            transaction {
                repository.save(issue)
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }

    @Test
    fun `save should not insert duplicate issue`() =
        runBlocking {
            val issue = createTestIssue()

            transaction {
                repository.save(issue)
                repository.save(issue) // Try to save the same issue again
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }

    @Test
    fun `exists should return false for non-existent issue`() =
        runBlocking {
            val result =
                transaction {
                    repository.exists(999, "nonexistent/repo")
                }
            assertFalse(result)
        }

    @Test
    fun `should handle issue with empty labels`() =
        runBlocking {
            val issue = createTestIssue(labels = emptyList())

            transaction {
                repository.save(issue)
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }

    @Test
    fun `should correctly extract repository name from URL`() =
        runBlocking {
            val issue = createTestIssue()

            transaction {
                repository.save(issue)
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }
}
