package io.github.vprud.adapter.telegram

import com.github.kotlintelegrambot.errors.TelegramError

interface ErrorHandler {
    fun handleError(error: TelegramError)
}

class LoggingTelegramErrorHandler : ErrorHandler {
    override fun handleError(error: TelegramError) {
        println(error.getErrorMessage())
    }
}
