package io.github.vprud

import com.github.kotlintelegrambot.errors.TelegramError
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramBotServiceImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var commandHandlers: Map<String, CommandHandler>
    private lateinit var errorHandler: ErrorHandler
    private lateinit var issueUpdateChecker: IssueUpdateChecker
    private lateinit var tracker: GitHubIssueTracker
    private lateinit var botService: TelegramBotServiceImpl

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @BeforeEach
    fun setupEach() {
        commandHandlers =
            mapOf(
                "start" to mockk(),
                "help" to mockk(),
                "subscribe" to mockk(),
                "unsubscribe" to mockk(),
                "mysubscriptions" to mockk(),
            )
        errorHandler = mockk()
        issueUpdateChecker = mockk()
        tracker = mockk()
        botService = TelegramBotServiceImpl(commandHandlers, errorHandler, issueUpdateChecker)
    }

    @AfterEach
    fun tearDownEach() {
        clearAllMocks()
    }

    @Test
    fun `startBot should initialize bot and start polling`() {
        val token = "test-token"
        coEvery { issueUpdateChecker.startChecking(any()) } just Runs

        botService.startBot(token, tracker)

        coVerify { issueUpdateChecker.startChecking(any()) }
    }

    @Test
    fun `stopBot should stop polling and issue checking`() {
        val token = "test-token"
        coEvery { issueUpdateChecker.startChecking(any()) } just Runs
        coEvery { issueUpdateChecker.stopChecking() } just Runs
        botService.startBot(token, tracker)

        botService.stopBot()

        coVerify { issueUpdateChecker.stopChecking() }
    }

    @Test
    fun `should handle error messages correctly`() {
        val token = "test-token"
        val error = mockk<TelegramError>()
        coEvery { errorHandler.handleError(error) } just Runs
        coEvery { issueUpdateChecker.startChecking(any()) } just Runs

        botService.startBot(token, tracker)

        assertDoesNotThrow {
            errorHandler.handleError(error)
        }
    }
}
