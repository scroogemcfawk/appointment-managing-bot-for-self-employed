package dev.scroogemcfawk.manicurebot

import dev.scroogemcfawk.manicurebot.config.Config
import kotlinx.serialization.json.Json
import org.tinylog.Logger
import java.io.File

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {

    val json = Json { ignoreUnknownKeys = true }

    val config = try {
        json.decodeFromString(Config.serializer(), File(args.first()).readText())
    } catch (e: Exception) {
        Logger.error{"Failed get config: ${e.message}"}
        return
    }

    val con = try {
        val dbManager = DbManager(config.dataSourceUrl)
        dbManager.initDataBase()
//        dbManager.dropData()

         dbManager.con
    } catch (e: Exception) {
        Logger.error{"Failed database initialization: ${e.message}"}
        return
    }

    val bot = Bot(config, con)

    try {
        bot.run().join()
    } catch (e: Exception) {
        Logger.error{ e.message }
    }
}
