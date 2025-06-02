package io.github.vprud.task

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class RecurrentTaskExecutorTest {
    @AfterEach
    fun tearDown() {
        // Ensure all tasks are stopped after each test
        executor?.stop()
    }

    private var executor: RecurrentTaskExecutor? = null

    @Test
    fun `should execute task immediately after start`() =
        runBlocking {
            // Given
            val task = mockk<Task>()
            val notify = mockk<Notify>()
            executor = RecurrentTaskExecutor(task, checkIntervalMillis = 1000)

            // When
            executor?.start(notify)
            delay(100) // Give some time for the task to start

            // Then
            coVerify { task.run(notify) }
        }

    @Test
    fun `should stop task execution`() =
        runBlocking {
            // Given
            val task = mockk<Task>()
            val notify = mockk<Notify>()
            executor = RecurrentTaskExecutor(task, checkIntervalMillis = 100)

            // When
            executor?.start(notify)
            delay(150) // Wait for first execution
            executor?.stop()
            delay(200) // Wait to ensure no more executions

            // Then
            coVerify(exactly = 1) { task.run(notify) }
        }

    @Test
    fun `should handle task execution errors`() =
        runBlocking {
            // Given
            val task = mockk<Task>()
            val notify = mockk<Notify>()
            executor = RecurrentTaskExecutor(task, checkIntervalMillis = 100)

            coEvery { task.run(notify) } throws RuntimeException("Test error")

            // When
            executor?.start(notify)
            delay(350) // Wait for multiple executions

            // Then
            coVerify(atLeast = 1) { task.run(notify) }
        }
}
