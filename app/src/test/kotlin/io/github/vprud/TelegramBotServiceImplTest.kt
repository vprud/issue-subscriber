package io.github.vprud

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramBotServiceImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockBot = mockk<Bot>(relaxed = true)
    private val mockTracker = mockk<GitHubIssueTracker>()
    private val mockCommandHandler = mockk<CommandHandler>()
    private val mockErrorHandler = mockk<ErrorHandler>()
    private val mockIssueUpdateChecker = mockk<IssueUpdateChecker>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(ChatId::class)
        every { ChatId.fromId(any()) } returns mockk()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `startBot should initialize bot with correct parameters`() {
        val token = "test_token"
        val commandHandlers = mapOf("test" to mockCommandHandler)
        val service = TelegramBotServiceImpl(commandHandlers, mockErrorHandler, mockIssueUpdateChecker)

        mockkConstructor(Bot::class)
        every { anyConstructed<Bot>().startPolling() } just Runs

        service.startBot(token, mockTracker)

        verify { anyConstructed<Bot>().startPolling() }
        coVerify { mockIssueUpdateChecker.startChecking(any()) }
    }

    @Test
    fun `startBot should handle updates and send notifications`() =
        testScope.runTest {
            val token = "test_token"
            val commandHandlers = mapOf("test" to mockCommandHandler)
            val service = TelegramBotServiceImpl(commandHandlers, mockErrorHandler, mockIssueUpdateChecker)
            val chatId = 123L
            val issue = mockk<GitHubIssue>()

            every { issue.repositoryUrl } returns "https://api.github.com/repos/owner/repo"
            every { issue.title } returns "Test Issue"
            every { issue.labels } returns emptyList()
            every { issue.htmlUrl } returns "https://github.com/owner/repo/issues/1"

            mockkConstructor(Bot::class)
            every { anyConstructed<Bot>().startPolling() } just Runs

            service.startBot(token, mockTracker)

            val captor = slot<(Long, GitHubIssue) -> Unit>()
            verify { mockIssueUpdateChecker.startChecking(capture(captor)) }

            // Simulate update checker finding a new issue
            captor.captured.invoke(chatId, issue)

            verify {
                anyConstructed<Bot>().sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text =
                        """
                        New issue in owner/repo
                        Title: Test Issue
                        Labels: 
                        URL: https://github.com/owner/repo/issues/1
                        """.trimIndent(),
                )
            }
        }

    @Test
    fun `stopBot should stop polling and checking updates`() {
        val service = TelegramBotServiceImpl(emptyMap(), mockErrorHandler, mockIssueUpdateChecker)
        every { mockBot.stopPolling() } just Runs
        every { mockIssueUpdateChecker.stopChecking() } just Runs

        service.stopBot()

        verify { mockIssueUpdateChecker.stopChecking() }
    }
}
