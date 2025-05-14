package io.github.vprud

import io.github.vprud.table.SubscriptionTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class SubscriptionRepositoryImplTest {
    private lateinit var repository: SubscriptionRepositoryImpl
    val schema = Schema("test")
    val dbConfig =
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
            SchemaUtils.create(SubscriptionTable)
        }

        repository = SubscriptionRepositoryImpl()
    }

    @AfterEach
    fun tearDown() {
        transaction {
            SchemaUtils.drop(SubscriptionTable)
            SchemaUtils.dropSchema(schema, cascade = true)
        }
    }

    @Test
    fun `add should insert subscription and return inserted count`() =
        runBlocking {
            val subscription =
                Subscription(
                    chatId = 123L,
                    repository = "owner/repo",
                    labels = setOf("bug", "feature"),
                    lastCheckedIssueId = 100,
                )

            val result =
                newSuspendedTransaction {
                    SchemaUtils.setSchema(schema)
                    repository.add(subscription)
                }

            assertEquals(1, result)
        }

    @Test
    fun `get should return subscription if exists`() =
        runBlocking {
            val subscription =
                Subscription(
                    chatId = 123L,
                    repository = "owner/repo",
                    labels = setOf("bug"),
                    lastCheckedIssueId = 100,
                )

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.add(subscription)
                val result = repository.get(123L, "owner/repo")

                assertNotNull(result)
                assertEquals(123L, result.chatId)
                assertEquals("owner/repo", result.repository)
                assertEquals(setOf("bug"), result.labels)
                assertEquals(100, result.lastCheckedIssueId)
            }
        }

    @Test
    fun `get should return null if subscription not exists`() =
        runBlocking {
            val result =
                newSuspendedTransaction {
                    SchemaUtils.setSchema(schema)
                    repository.get(999L, "nonexistent/repo")
                }

            assertNull(result)
        }

    @Test
    fun `remove should return true if subscription was deleted`() =
        runBlocking {
            val subscription =
                Subscription(
                    chatId = 123L,
                    repository = "owner/repo",
                )

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.add(subscription)
                val result = repository.remove(123L, "owner/repo")

                assertTrue(result)
            }
        }

    @Test
    fun `remove should return false if subscription not exists`() =
        runBlocking {
            val result =
                newSuspendedTransaction {
                    SchemaUtils.setSchema(schema)
                    repository.remove(999L, "nonexistent/repo")
                }

            assertFalse(result)
        }

    @Test
    fun `getAll should return all subscriptions`() =
        runBlocking {
            val subscriptions =
                listOf(
                    Subscription(123L, "owner/repo1"),
                    Subscription(123L, "owner/repo2", setOf("bug")),
                    Subscription(456L, "owner/repo3"),
                )

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                subscriptions.forEach { repository.add(it) }
                val result = repository.getAll()

                assertEquals(3, result.size)
                assertTrue(result.any { it.repository == "owner/repo1" && it.labels.isEmpty() })
                assertTrue(result.any { it.repository == "owner/repo2" && it.labels == setOf("bug") })
                assertTrue(result.any { it.repository == "owner/repo3" && it.chatId == 456L })
            }
        }

    @Test
    fun `getByChatId should return subscriptions only for specified chat`() =
        runBlocking {
            val subscriptions =
                listOf(
                    Subscription(123L, "owner/repo1"),
                    Subscription(123L, "owner/repo2"),
                    Subscription(456L, "owner/repo3"),
                )

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                subscriptions.forEach { repository.add(it) }
                val result = repository.getByChatId(123L)

                assertEquals(2, result.size)
                assertTrue(result.all { it.chatId == 123L })
            }
        }

    @Test
    fun `updateLastChecked should update last checked issue id`() =
        runBlocking {
            val subscription =
                Subscription(
                    chatId = 123L,
                    repository = "owner/repo",
                    lastCheckedIssueId = 100,
                )

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.add(subscription)
                repository.updateLastChecked(123L, "owner/repo", 150)
                val updated = repository.get(123L, "owner/repo")

                assertNotNull(updated)
                assertEquals(150, updated.lastCheckedIssueId)
            }
        }

    @Test
    fun `should handle empty labels correctly`() =
        runBlocking {
            val subscription =
                Subscription(
                    chatId = 123L,
                    repository = "owner/repo",
                    labels = emptySet(),
                )

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.add(subscription)
                val result = repository.get(123L, "owner/repo")

                assertNotNull(result)
                assertTrue(result.labels.isEmpty())
            }
        }

    @Test
    fun `should handle null lastCheckedIssueId correctly`() =
        runBlocking {
            val subscription =
                Subscription(
                    chatId = 123L,
                    repository = "owner/repo",
                    lastCheckedIssueId = null,
                )

            newSuspendedTransaction {
                SchemaUtils.setSchema(schema)
                repository.add(subscription)
                val result = repository.get(123L, "owner/repo")

                assertNotNull(result)
                assertNull(result.lastCheckedIssueId)
            }
        }
}
