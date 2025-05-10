package io.github.vprud

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.errors.TelegramError
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramBotServiceTest {
    private val mockBot = mockk<Bot>(relaxed = true)
    private val mockTracker = mockk<GitHubIssueTracker>()
    private val mockErrorHandler = mockk<ErrorHandler>()
    private val mockIssueUpdateChecker = mockk<IssueUpdateChecker>()
    private val mockCommandHandlers =
        mapOf(
            "test" to mockk<CommandHandler>(),
        )

    private val service =
        TelegramBotServiceImpl(
            commandHandlers = mockCommandHandlers,
            errorHandler = mockErrorHandler,
            issueUpdateChecker = mockIssueUpdateChecker,
        )

    @BeforeTest
    fun setup() {
        mockkStatic("com.github.kotlintelegrambot.BotKt")
        every { anyConstructed<Bot>().startPolling() } just Runs
        every { mockCommandHandlers["test"]!!.handleCommand(any(), any(), any()) } just Runs
        every { mockErrorHandler.handleError(any()) } just Runs
        every { mockIssueUpdateChecker.startChecking(any()) } just Runs
        every { mockIssueUpdateChecker.stopChecking() } just Runs
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startBot should initialize bot with handlers`() =
        runTest {
            every { bot(any()) } returns mockBot

            service.startBot("test_token", mockTracker)

            verify { mockIssueUpdateChecker.startChecking(any()) }
            verify { mockBot.startPolling() }
        }

    @Test
    fun `stopBot should stop polling and checking updates`() {
        service.stopBot()

        verify { mockIssueUpdateChecker.stopChecking() }
        verify { mockBot.stopPolling() }
    }

    @Test
    fun `bot should handle commands with registered handlers`() =
        runTest {
            every { bot(any()) } answers {
                val init: Bot.() -> Unit = firstArg()
                mockBot.init()
                mockBot
            }

            service.startBot("test_token", mockTracker)

            verify { mockCommandHandlers["test"]!!.handleCommand(mockBot, any(), mockTracker) }
        }

    @Test
    fun `bot should handle errors with error handler`() =
        runTest {
            val mockError = mockk<TelegramError>()
            every { bot(any()) } answers {
                val init: Bot.() -> Unit = firstArg()
                mockBot.init()
                mockBot
            }
            every { mockBot.dispatch } answers {
                val dispatch: Bot.() -> Unit = firstArg()
                mockBot.dispatch()
                mockBot
            }
            every { mockBot.telegramError(any()) } answers {
                val handler: (TelegramError) -> Unit = firstArg()
                handler(mockError)
                mockBot
            }

            service.startBot("test_token", mockTracker)

            verify { mockErrorHandler.handleError(mockError) }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class IssueUpdateCheckerTest {
    private val mockTracker = mockk<GitHubIssueTracker>()
    private val checker = IssueUpdateChecker(mockTracker, 100) // Short interval for tests

    @BeforeTest
    fun setup() {
        coEvery { mockTracker.checkForUpdates(any()) } just Runs
    }

    @Test
    fun `startChecking should periodically check for updates`() =
        runTest {
            val notifyFn: (Long, GitHubIssue) -> Unit = mockk(relaxed = true)

            checker.startChecking(notifyFn)
            advanceTimeBy(250) // Let it run for a while

            coVerify(atLeast = 2) { mockTracker.checkForUpdates(notifyFn) }

            checker.stopChecking()
        }

    @Test
    fun `stopChecking should cancel the checking job`() =
        runTest {
            val notifyFn: (Long, GitHubIssue) -> Unit = mockk(relaxed = true)

            checker.startChecking(notifyFn)
            checker.stopChecking()
            advanceTimeBy(250)

            coVerify(exactly = 0) { mockTracker.checkForUpdates(any()) }
        }
}

class CreateDefaultBotServiceTest {
    @Test
    fun `createDefaultBotService should create service with all command handlers`() {
        val mockChecker = mockk<IssueUpdateChecker>()
        val service = createDefaultBotService(mockChecker)

        assertTrue(service is TelegramBotServiceImpl)
        val impl = service as TelegramBotServiceImpl
        assertEquals(5, impl.commandHandlers.size)
        assertTrue(impl.commandHandlers.containsKey("start"))
        assertTrue(impl.commandHandlers.containsKey("help"))
        assertTrue(impl.commandHandlers.containsKey("subscribe"))
        assertTrue(impl.commandHandlers.containsKey("unsubscribe"))
        assertTrue(impl.commandHandlers.containsKey("mysubscriptions"))
        assertTrue(impl.errorHandler is LoggingTelegramErrorHandler)
        assertEquals(mockChecker, impl.issueUpdateChecker)
    }
}
