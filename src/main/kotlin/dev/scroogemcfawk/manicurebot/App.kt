package dev.scroogemcfawk.manicurebot

import kotlinx.serialization.builtins.serializer
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.io.path.Path

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {

    val json = Json { ignoreUnknownKeys = true }
    val config = json.decodeFromString(Config.serializer(), File(args.first()).readText())
    val locale = json.decodeFromString(Locale.serializer(), File(config.locale).readText())
    val bot = Bot(config.token, locale, config.dev, config.owner)

    bot.run().join()

}