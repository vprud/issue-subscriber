package io.github.vprud

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(ExtensionDatabase::class)
class SubscriptionRepositoryImplTest {
    private val repository = SubscriptionRepositoryImpl()

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
                transaction {
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

            transaction {
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
                transaction {
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
                    labels = setOf("bug"),
                )

            transaction {
                repository.add(subscription)
                val result = repository.remove(123L, "owner/repo")

                assertTrue(result)
            }
        }

    @Test
    fun `remove should return false if subscription not exists`() =
        runBlocking {
            val result =
                transaction {
                    repository.remove(999L, "nonexistent/repo")
                }

            assertFalse(result)
        }

    @Test
    fun `getAll should return all subscriptions`() =
        runBlocking {
            val subscriptions =
                listOf(
                    Subscription(123L, "owner/repo1", setOf("bug")),
                    Subscription(123L, "owner/repo2", setOf("feature")),
                    Subscription(456L, "owner/repo3", setOf("enhancement")),
                )

            transaction {
                subscriptions.forEach { repository.add(it) }
                val result = repository.getAll()

                assertEquals(3, result.size)
                assertTrue(result.any { it.repository == "owner/repo1" && it.labels == setOf("bug") })
                assertTrue(result.any { it.repository == "owner/repo2" && it.labels == setOf("feature") })
                assertTrue(result.any { it.repository == "owner/repo3" && it.chatId == 456L })
            }
        }

    @Test
    fun `getByChatId should return subscriptions only for specified chat`() =
        runBlocking {
            val subscriptions =
                listOf(
                    Subscription(123L, "owner/repo1", setOf("bug")),
                    Subscription(123L, "owner/repo2", setOf("feature")),
                    Subscription(456L, "owner/repo3", setOf("enhancement")),
                )

            transaction {
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
                    labels = setOf("bug"),
                    lastCheckedIssueId = 100,
                )

            transaction {
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

            transaction {
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
                    labels = setOf("bug"),
                    lastCheckedIssueId = null,
                )

            transaction {
                repository.add(subscription)
                val result = repository.get(123L, "owner/repo")

                assertNotNull(result)
                assertNull(result.lastCheckedIssueId)
            }
        }
}
