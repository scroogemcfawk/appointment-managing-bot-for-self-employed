package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import java.io.File
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {

    val json = Json { ignoreUnknownKeys = true }
    val config: Config = json.decodeFromString(Config.serializer(), File(args.first()).readText())
    val bot = telegramBot(config.token)
    val scope = CoroutineScope(Dispatchers.Default)

    bot.buildBehaviourWithLongPolling(scope) {
        val me = getMe()

        onCommand("start", requireOnlyCommandInMessage = true) {
            send(it.chat.id, "Privet hui")
        }

        onCommand("/help", requireOnlyCommandInMessage = false) {
            execute ( it.content.createResend(it.chat.id) )
        }

        println(me)
    }.join()
}