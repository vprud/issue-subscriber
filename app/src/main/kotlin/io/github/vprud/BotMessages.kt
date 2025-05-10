package io.github.vprud

object BotMessages {
    val COMMAND_LIST =
        """
        Available commands:
        
        /subscribe <owner/repo> \[labels] - subscribe to repository updates (tags separated by commas)
        /unsubscribe <owner/repo> - unsubscribe from a repository
        /mysubscriptions - show current subscriptions
        /help - show this message
        """.trimIndent()

    val WELCOME_MESSAGE =
        """
        Hi 👋
        
        I'm a bot for tracking new GitHub repository issues.
        
        Available commands:
        
        /subscribe <owner/repo> \[labels] - subscribe to repository updates (tags separated by commas)
        /unsubscribe <owner/repo> - unsubscribe from a repository
        /mysubscriptions - show current subscriptions
        /help - show this message
        """.trimIndent()
}
