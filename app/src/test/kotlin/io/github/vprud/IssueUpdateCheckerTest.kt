package io.github.vprud

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Disabled
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IssueUpdateCheckerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockTracker = mockk<GitHubIssueTracker>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Disabled("Need fix test itself")
    @Test
    fun `startChecking should periodically check for updates`() =
        testScope.runTest {
            val checker = IssueUpdateChecker(mockTracker, checkIntervalMillis = 100)
            val notify: (Long, GitHubIssue) -> Unit = mockk(relaxed = true)

            checker.startChecking(notify)

            // Allow time for at least one check
            testScope.advanceTimeBy(150)

            coVerify { mockTracker.checkForUpdates(notify) }

            checker.stopChecking()
        }

    @Disabled("Need fix test itself")
    @Test
    fun `stopChecking should cancel the checking job`() =
        testScope.runTest {
            val checker = IssueUpdateChecker(mockTracker)
            val notify: (Long, GitHubIssue) -> Unit = mockk(relaxed = true)

            checker.startChecking(notify)
            checker.stopChecking()

            testScope.advanceTimeBy(1000)

            coVerify(exactly = 0) { mockTracker.checkForUpdates(notify) }
        }
}
