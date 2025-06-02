package io.github.vprud

import io.github.cdimascio.dotenv.dotenv
import io.github.vprud.adapter.telegram.createDefaultBotService
import io.github.vprud.databse.Database
import io.github.vprud.databse.DbConfig
import io.github.vprud.provider.database.IssueRepositoryImpl
import io.github.vprud.provider.database.SubscriptionRepositoryImpl
import io.github.vprud.provider.github.GitHubClientImpl
import io.github.vprud.service.IssueCheckServiceImpl
import io.github.vprud.service.IssueUpdatesTrackerTask
import io.github.vprud.service.SubscriptionServiceImpl
import io.github.vprud.task.RecurrentTaskExecutor
import okhttp3.OkHttpClient

class App {
    val greeting: String
        get() {
            return "Hello World! from ${System.getProperty("java.vendor")} ${System.getProperty("java.version")}"
        }
}

fun main() {
    val env = dotenv { ignoreIfMissing = true }

    val tgToken = env["TG_TOKEN"] ?: error("TG_TOKEN not set in .env")
    val githubToken = dotenv()["GITHUB_TOKEN"] ?: error("GITHUB_TOKEN not set in .env")
    val dbConfig =
        DbConfig(
            url = env["DB_URL"] ?: error("DB_URL not set in .env"),
            user = env["DB_USER"] ?: error("DB_USER not set in .env"),
            password = env["DB_PASSWORD"] ?: error("DB_PASSWORD not set in .env"),
        )

    Database.init(dbConfig)

    val subscriptionRepository = SubscriptionRepositoryImpl()
    val issueRepository = IssueRepositoryImpl()
    val httpClient = OkHttpClient()
    val gitHubClient = GitHubClientImpl(httpClient, githubToken)
    val issueCheckService = IssueCheckServiceImpl(gitHubClient, subscriptionRepository, issueRepository)
    val subscriptionService = SubscriptionServiceImpl(subscriptionRepository)
    val trackerTask = IssueUpdatesTrackerTask(subscriptionRepository, issueCheckService)
    val checker = RecurrentTaskExecutor(trackerTask)
    val botService = createDefaultBotService(checker)

    botService.startBot(
        tgToken,
        subscriptionService,
    )
}
