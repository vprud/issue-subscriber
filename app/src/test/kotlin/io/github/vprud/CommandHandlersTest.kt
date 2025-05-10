package io.github.vprud

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest

class CommandHandlersTest {
    private val mockBot = mockk<Bot>()
    private val mockMessage = mockk<Message>()
    private val mockTracker = mockk<GitHubIssueTracker>()

    @BeforeTest
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `StartCommandHandler should send welcome message`() =
        runTest {
            val handler = StartCommandHandler()
            every { mockMessage.chat.id } returns 123L
            every { mockBot.sendMessage(any(), any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            verify {
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = BotMessages.WELCOME_MESSAGE,
                    parseMode = ParseMode.MARKDOWN,
                )
            }
        }

    @Test
    fun `HelpCommandHandler should send command list`() =
        runTest {
            val handler = HelpCommandHandler()
            every { mockMessage.chat.id } returns 123L
            every { mockBot.sendMessage(any(), any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            verify {
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = BotMessages.COMMAND_LIST,
                    parseMode = ParseMode.MARKDOWN,
                )
            }
        }

    @Test
    fun `SubscribeCommandHandler should handle valid subscription`() =
        runTest {
            val handler = SubscribeCommandHandler()
            every { mockMessage.chat.id } returns 123L
            every { mockMessage.text } returns "/subscribe owner/repo label1,label2"
            every { mockTracker.subscribe(any(), any(), any()) } just Runs
            every { mockBot.sendMessage(any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            verify {
                mockTracker.subscribe(123L, "owner/repo", setOf("label1", "label2"))
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = "Subscribed to owner/repo with labels: label1, label2",
                )
            }
        }

    @Test
    fun `SubscribeCommandHandler should handle missing args`() =
        runTest {
            val handler = SubscribeCommandHandler()
            every { mockMessage.chat.id } returns 123L
            every { mockMessage.text } returns "/subscribe"
            every { mockBot.sendMessage(any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            verify(exactly = 0) { mockTracker.subscribe(any(), any(), any()) }
            verify {
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = "Usage: /subscribe <owner/repo> [tags]",
                )
            }
        }

    @Test
    fun `SubscribeCommandHandler should handle subscription error`() =
        runTest {
            val handler = SubscribeCommandHandler()
            every { mockMessage.chat.id } returns 123L
            every { mockMessage.text } returns "/subscribe owner/repo"
            every { mockTracker.subscribe(any(), any(), any()) } throws RuntimeException("API error")
            every { mockBot.sendMessage(any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            verify {
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = "Error: API error",
                )
            }
        }

    @Test
    fun `UnsubscribeCommandHandler should handle successful unsubscribe`() =
        runTest {
            val handler = UnsubscribeCommandHandler()
            every { mockMessage.chat.id } returns 123L
            every { mockMessage.text } returns "/unsubscribe owner/repo"
            every { mockTracker.unsubscribe(any(), any()) } returns true
            every { mockBot.sendMessage(any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            verify {
                mockTracker.unsubscribe(123L, "owner/repo")
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = "Unsubscribed from owner/repo",
                )
            }
        }

    @Test
    fun `UnsubscribeCommandHandler should handle missing subscription`() =
        runTest {
            val handler = UnsubscribeCommandHandler()
            every { mockMessage.chat.id } returns 123L
            every { mockMessage.text } returns "/unsubscribe owner/repo"
            every { mockTracker.unsubscribe(any(), any()) } returns false
            every { mockBot.sendMessage(any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            verify {
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = "You were not subscribed to owner/repo",
                )
            }
        }

    @Test
    fun `MySubscriptionsCommandHandler should show no subscriptions message`() =
        runTest {
            val handler = MySubscriptionsCommandHandler()
            every { mockMessage.chat.id } returns 123L
            every { mockTracker.getSubscriptions(any()) } returns emptyList()
            every { mockBot.sendMessage(any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            verify {
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = "You have no active subscriptions",
                )
            }
        }

    @Test
    fun `MySubscriptionsCommandHandler should show subscriptions list`() =
        runTest {
            val handler = MySubscriptionsCommandHandler()
            val subscriptions =
                listOf(
                    Subscription(123L, "owner/repo1", setOf("label1")),
                    Subscription(123L, "owner/repo2", emptySet()),
                )

            every { mockMessage.chat.id } returns 123L
            every { mockTracker.getSubscriptions(any()) } returns subscriptions
            every { mockBot.sendMessage(any(), any()) } returns mockk()

            handler.handleCommand(mockBot, mockMessage, mockTracker)

            val expectedText =
                """
                Your subscriptions:
                
                Repository: owner/repo1
                Labels: label1
                
                Repository: owner/repo2
                Labels: all
                """.trimIndent()

            verify {
                mockBot.sendMessage(
                    chatId = ChatId.fromId(123L),
                    text = expectedText,
                )
            }
        }
}
