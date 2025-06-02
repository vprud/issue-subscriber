package io.github.vprud.adapter.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import io.github.vprud.service.SubscriptionService

interface CommandHandler {
    fun handleCommand(
        bot: Bot,
        message: Message,
        subscriptionService: SubscriptionService,
    )
}

class StartCommandHandler : CommandHandler {
    override fun handleCommand(
        bot: Bot,
        message: Message,
        subscriptionService: SubscriptionService,
    ) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = BotMessages.WELCOME_MESSAGE,
            parseMode = ParseMode.MARKDOWN,
        )
    }
}

class HelpCommandHandler : CommandHandler {
    override fun handleCommand(
        bot: Bot,
        message: Message,
        subscriptionService: SubscriptionService,
    ) {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = BotMessages.COMMAND_LIST,
            parseMode = ParseMode.MARKDOWN,
        )
    }
}

class SubscribeCommandHandler : CommandHandler {
    override fun handleCommand(
        bot: Bot,
        message: Message,
        subscriptionService: SubscriptionService,
    ) {
        val args = message.text?.split(" ")?.drop(1) ?: emptyList()
        if (args.isEmpty()) {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "Usage: /subscribe <owner/repo> [tags]",
            )
            return
        }

        val repo = args[0]
        val labels =
            args
                .getOrNull(1)
                ?.split(",")
                ?.map { it.trim() }
                ?.toSet() ?: emptySet()

        try {
            subscriptionService.subscribe(message.chat.id, repo, labels)
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text =
                    "Subscribed to $repo" +
                        if (labels.isNotEmpty()) " with labels: ${labels.joinToString()}" else "",
            )
        } catch (e: Exception) {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "Error: ${e.message ?: "Failed to subscribe"}",
            )
        }
    }
}

class UnsubscribeCommandHandler : CommandHandler {
    override fun handleCommand(
        bot: Bot,
        message: Message,
        subscriptionService: SubscriptionService,
    ) {
        val args = message.text?.split(" ")?.drop(1) ?: emptyList()
        if (args.isEmpty()) {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "Usage: /unsubscribe <owner/repo>",
            )
            return
        }

        val repo = args[0]
        val success = subscriptionService.unsubscribe(message.chat.id, repo)

        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = if (success) "Unsubscribed from $repo" else "You were not subscribed to $repo",
        )
    }
}

class MySubscriptionsCommandHandler : CommandHandler {
    override fun handleCommand(
        bot: Bot,
        message: Message,
        subscriptionService: SubscriptionService,
    ) {
        val subscriptions = subscriptionService.getSubscriptions(message.chat.id)
        if (subscriptions.isEmpty()) {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "You have no active subscriptions",
            )
            return
        }

        val messageText =
            subscriptions.joinToString("\n\n") { sub ->
                "Repository: ${sub.repository}\n" +
                    "Labels: ${if (sub.labels.isEmpty()) "all" else sub.labels.joinToString()}"
            }

        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "Your subscriptions:\n\n$messageText",
        )
    }
}
