package io.github.vprud

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel

interface TelegramBotService {
    fun startBot(
        tgToken: String,
        tracker: GitHubIssueTracker,
    )

    fun stopBot()
}

class TelegramBotServiceImpl(
    private val commandHandlers: Map<String, CommandHandler>,
    private val errorHandler: ErrorHandler,
    private val issueUpdateChecker: IssueUpdateChecker,
) : TelegramBotService {
    private var bot: Bot? = null

    override fun startBot(
        tgToken: String,
        tracker: GitHubIssueTracker,
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
                                tracker = tracker,
                            )
                        }
                    }

                    telegramError {
                        errorHandler.handleError(error)
                    }
                }
            }

        bot?.startPolling()

        issueUpdateChecker.startChecking { chatId, issue ->
            val messageText =
                """
                New issue in ${issue.repositoryUrl.split("/").takeLast(2).joinToString("/")}
                Title: ${issue.title}
                Labels: ${issue.labels.joinToString { it.name }}
                URL: ${issue.htmlUrl}
                """.trimIndent()

            bot?.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = messageText,
            )
        }
    }

    override fun stopBot() {
        issueUpdateChecker.stopChecking()
        bot?.stopPolling()
    }
}

fun createDefaultBotService(issueUpdateChecker: IssueUpdateChecker): TelegramBotService {
    val commandHandlers =
        mapOf(
            "start" to StartCommandHandler(),
            "help" to HelpCommandHandler(),
            "subscribe" to SubscribeCommandHandler(),
            "unsubscribe" to UnsubscribeCommandHandler(),
            "mysubscriptions" to MySubscriptionsCommandHandler(),
        )

    return TelegramBotServiceImpl(
        commandHandlers = commandHandlers,
        errorHandler = LoggingTelegramErrorHandler(),
        issueUpdateChecker = issueUpdateChecker,
    )
}
