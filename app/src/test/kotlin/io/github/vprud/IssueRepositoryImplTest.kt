package io.github.vprud

import io.github.vprud.table.IssueTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IssueRepositoryImplTest {
    private lateinit var repository: IssueRepositoryImpl
    private val schema = Schema("test")
    private val dbConfig =
        DbConfig(
            url = "jdbc:postgresql://localhost:5432/postgres",
            user = "postgres",
            password = "postgres",
        )

    @BeforeEach
    fun setup() {
        Database.connect(
            url = dbConfig.url,
            driver = "org.postgresql.Driver",
            user = dbConfig.user,
            password = dbConfig.password,
        )

        transaction {
            SchemaUtils.createSchema(schema)
            SchemaUtils.setSchema(schema)
            SchemaUtils.create(IssueTable)
        }

        repository = IssueRepositoryImpl()
    }

    @AfterEach
    fun tearDown() {
        transaction {
            SchemaUtils.drop(IssueTable)
            SchemaUtils.dropSchema(schema, cascade = true)
        }
    }

    private fun createTestIssue(
        number: Int = 123,
        body: String? = "Test issue body",
        labels: List<GitHubIssue.Label> =
            listOf(
                GitHubIssue.Label(
                    id = 1L,
                    url = "https://api.github.com/repos/owner/repo/labels/bug",
                    name = "bug",
                    color = "d73a4a",
                    description = "Bug label",
                ),
            ),
        milestone: GitHubIssue.Milestone? =
            GitHubIssue.Milestone(
                url = "https://api.github.com/repos/owner/repo/milestones/1",
                htmlUrl1 = "https://github.com/owner/repo/milestone/1",
                id = 1L,
                title = "Test Milestone",
                description = "Test milestone description",
            ),
    ) = GitHubIssue(
        url = "https://api.github.com/repos/owner/repo/issues/123",
        repositoryUrl = "https://api.github.com/repos/owner/repo",
        htmlUrl = "https://github.com/owner/repo/issues/123",
        number = number,
        title = "Test Issue",
        labels = labels,
        state = "open",
        milestone = milestone,
        comments = 0,
        createdAt = "2024-03-20T10:00:00Z",
        updatedAt = "2024-03-20T10:00:00Z",
        body = body,
    )

    @Test
    fun `save should insert new issue`() =
        runBlocking {
            val issue = createTestIssue()

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.save(issue)
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }

    @Test
    fun `save should not insert duplicate issue`() =
        runBlocking {
            val issue = createTestIssue()

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
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
                newSuspendedTransaction {
                    SchemaUtils.setSchema(schema)
                    repository.exists(999, "nonexistent/repo")
                }
            assertFalse(result)
        }

    @Test
    fun `should handle issue with null body`() =
        runBlocking {
            val issue = createTestIssue(body = null)

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.save(issue)
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }

    @Test
    fun `should handle issue with null milestone`() =
        runBlocking {
            val issue = createTestIssue(milestone = null)

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.save(issue)
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }

    @Test
    fun `should handle issue with empty labels`() =
        runBlocking {
            val issue = createTestIssue(labels = emptyList())

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.save(issue)
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }

    @Test
    fun `should correctly extract repository name from URL`() =
        runBlocking {
            val issue = createTestIssue()

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.save(issue)
                val exists = repository.exists(123, "owner/repo")
                assertTrue(exists)
            }
        }
}
