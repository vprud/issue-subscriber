package io.github.vprud.task

import kotlinx.coroutines.*

class RecurrentTaskExecutor(
    private val task: Task,
    private val checkIntervalMillis: Long = 1 * 60 * 1000,
) {
    private var checkJob: Job? = null

    fun start(notify: Notify) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        checkJob =
            scope.launch {
                while (isActive) {
                    task.run(notify)
                    delay(checkIntervalMillis)
                }
            }
    }

    fun stop() {
        checkJob?.cancel()
    }
}
