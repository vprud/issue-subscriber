package io.github.vprud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class IssueUpdateChecker(
    private val tracker: GitHubIssueTracker,
    private val checkIntervalMillis: Long = 1 * 60 * 1000,
) {
    private var checkJob: Job? = null

    fun startChecking(notify: (Long, GitHubIssue) -> Unit) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        checkJob =
            scope.launch {
                while (isActive) {
                    println("Checking for updates...")
                    tracker.checkForUpdates(notify)
                    delay(checkIntervalMillis)
                }
            }
    }

    fun stopChecking() {
        checkJob?.cancel()
    }
}
