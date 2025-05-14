package io.github.vprud

import io.github.vprud.table.SubscriptionTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    fun init(dbConfig: DbConfig) {
        val database =
            Database.connect(
                url = dbConfig.url,
                driver = "org.postgresql.Driver",
                user = dbConfig.user,
                password = dbConfig.password,
            )

        transaction(database) {
            SchemaUtils.create(SubscriptionTable)
        }
    }
}

data class DbConfig(
    val url: String,
    val user: String,
    val password: String,
)
