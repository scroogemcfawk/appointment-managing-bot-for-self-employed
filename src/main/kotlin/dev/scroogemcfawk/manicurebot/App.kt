package dev.scroogemcfawk.manicurebot

import dev.scroogemcfawk.manicurebot.config.Config
import java.io.File
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {

    val log = LoggerFactory.getLogger("App.kt")!!

    val json = Json { ignoreUnknownKeys = true }
    val config = json.decodeFromString(Config.serializer(), File(args.first()).readText())

    try {
        val dbManager = DbManager(config.dataSourceUrl)
        dbManager.initDataBase()
//        dbManager.dropData()

        val con = dbManager.con

        val bot = Bot(config, con)
        bot.run().join()

    } catch (e: Exception) {
        log.error("Initialization failed.")
    }


}
