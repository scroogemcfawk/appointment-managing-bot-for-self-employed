package dev.scroogemcfawk.manicurebot

import dev.scroogemcfawk.manicurebot.config.Config
import java.io.File
import kotlinx.serialization.json.Json

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {

    val json = Json { ignoreUnknownKeys = true }
    val config = json.decodeFromString(Config.serializer(), File(args.first()).readText())
    val bot = Bot(config)

    bot.run().join()

}