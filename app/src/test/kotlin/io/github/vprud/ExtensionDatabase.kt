package io.github.vprud

import io.github.vprud.table.IssueTable
import io.github.vprud.table.SubscriptionTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ExtensionDatabase :
    BeforeAllCallback,
    BeforeEachCallback,
    AfterEachCallback {
    private val dbConfig =
        DbConfig(
            url = "jdbc:postgresql://localhost:5432/postgres",
            user = "postgres",
            password = "postgres",
        )
    private val schema = Schema("test")

    override fun beforeAll(context: ExtensionContext?) {
        Database.connect(
            url = dbConfig.url,
            driver = "org.postgresql.Driver",
            user = dbConfig.user,
            password = dbConfig.password,
            databaseConfig =
                DatabaseConfig {
                    defaultSchema = schema
                },
        )
    }

    override fun beforeEach(context: ExtensionContext?) {
        transaction {
            SchemaUtils.createSchema(schema)
            SchemaUtils.create(SubscriptionTable)
            SchemaUtils.create(IssueTable)
        }
    }

    override fun afterEach(context: ExtensionContext?) {
        transaction {
            SchemaUtils.drop(SubscriptionTable)
            SchemaUtils.drop(IssueTable)
            SchemaUtils.dropSchema(schema, cascade = true)
        }
    }
}
