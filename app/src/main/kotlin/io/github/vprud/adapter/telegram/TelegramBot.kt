package io.github.vprud.adapter.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel
import io.github.vprud.service.SubscriptionService
import io.github.vprud.task.RecurrentTaskExecutor

interface TelegramBot {
    fun startBot(
        tgToken: String,
        subscriptionService: SubscriptionService,
    )

    fun stopBot()
}

class TelegramBotImpl(
    private val commandHandlers: Map<String, CommandHandler>,
    private val errorHandler: ErrorHandler,
    private val recurrentTaskExecutor: RecurrentTaskExecutor,
) : TelegramBot {
    private var bot: Bot? = null

    override fun startBot(
        tgToken: String,
        subscriptionService: SubscriptionService,
    ) {
        bot =
            bot {
                token = tgToken
                timeout = 30
                logLevel = LogLevel.Network.Body

                dispatch {
                    commandHandlers.forEach { (command, handler) ->
                        command(command) {
                            handler.handleCommand(
                                bot = bot,
                                message = message,
                                subscriptionService = subscriptionService,
                            )
                        }
                    }

                    telegramError {
                        errorHandler.handleError(error)
                    }
                }
            }

        bot?.startPolling()

        recurrentTaskExecutor.start { chatId, issue ->
            val messageText =
                """
                New issue in ${issue.repositoryUrl.split("/").takeLast(2).joinToString("/")}
                Title: ${issue.title}
                Labels: ${issue.labels.joinToString { it }}
                URL: ${issue.htmlUrl}
                """.trimIndent()

            bot?.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = messageText,
            )
        }
    }

    override fun stopBot() {
        recurrentTaskExecutor.stop()
        bot?.stopPolling()
    }
}

fun createDefaultBotService(recurrentTaskExecutor: RecurrentTaskExecutor): TelegramBot {
    val commandHandlers =
        mapOf(
            "start" to StartCommandHandler(),
            "help" to HelpCommandHandler(),
            "subscribe" to SubscribeCommandHandler(),
            "unsubscribe" to UnsubscribeCommandHandler(),
            "mysubscriptions" to MySubscriptionsCommandHandler(),
        )

    return TelegramBotImpl(
        commandHandlers = commandHandlers,
        errorHandler = LoggingTelegramErrorHandler(),
        recurrentTaskExecutor = recurrentTaskExecutor,
    )
}
