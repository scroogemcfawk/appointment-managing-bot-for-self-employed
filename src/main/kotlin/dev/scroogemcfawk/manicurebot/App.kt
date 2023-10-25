package dev.scroogemcfawk.manicurebot

import dev.scroogemcfawk.manicurebot.config.Config
import java.io.File
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {

    val log = try {
        LoggerFactory.getLogger("App.kt")
    } catch (e: Exception) {
        throw Exception("Failed to get logger: ${e.message}")
    }

    val json = Json { ignoreUnknownKeys = true }

    val config = try {
        json.decodeFromString(Config.serializer(), File(args.first()).readText())
    } catch (e: Exception) {
        log.error("Failed get config: ${e.message}")
        throw Exception("Failed get config: ${e.message}")
    }

    try {
        val dbManager = DbManager(config.dataSourceUrl)
        dbManager.initDataBase()
//        dbManager.dropData()

        val con = dbManager.con

        val bot = Bot(config, con)
        bot.run().join()

    } catch (e: Exception) {
        log.error("Failed database initialization: ${e.message}")
    }


}
