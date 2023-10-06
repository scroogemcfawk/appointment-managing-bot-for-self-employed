package dev.scroogemcfawk.manicurebot

import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.modify.setChatPhoto
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.buttonField
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.buttons.reply.simpleReplyButton
import dev.inmo.tgbotapi.types.chat.CommonBot
import dev.inmo.tgbotapi.types.chat.ExtendedBot
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.flatMatrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class Bot(token: String, val locale: Locale) {

    val bot = telegramBot(token)
    val scope = CoroutineScope(Dispatchers.Default)
    val log = LoggerFactory.getLogger(Bot::class.java)

    val dev = ChatId(471271839)

    suspend fun run(): Job {
        return bot.buildBehaviourWithLongPolling(scope) {
            val me = getMe()

            onCommand("start", requireOnlyCommandInMessage = true) {
                send(it.chat.id, locale.startMessage)
            }

            onCommand("help", requireOnlyCommandInMessage = true) {
                send(it.chat.id, locale.helpMessage)
            }

            onCommand("t", requireOnlyCommandInMessage = true) {
                println(bot.sendMessage(
                    chatId = it.chat.id,
                    text = "ktgbotapi is the best Kotlin Telegram Bot API library",
                    replyMarkup = InlineKeyboardMarkup(
                        keyboard = listOf(
                            listOf(
                                dataInlineButton("Ok", "ok")
                            ),
                        )
                    ),
                ))
            }

            onCommand("myid", requireOnlyCommandInMessage = true) {
                val chat = it.chat
                send(
                    dev, "${it.chat.id.chatId} (${it.chat.asPrivateChat()?.firstName} ${
                        it.chat
                            .asPrivateChat()?.lastName
                    } ${it.chat.asPrivateChat()?.username?.username})"
                )

            }
            onCommand("addAppointment") { msg ->
//                reply(msg)
            }

            onUnhandledCommand {
                reply(
                    it,
                    replyMarkup = InlineKeyboardMarkup(
                        keyboard = flatMatrix {
                            +CallbackDataInlineKeyboardButton("1", "1")
                        }
                    )

                ) {
                    + "asdf"
                }
            }

            log.info("Bot is initialized and running: ")
            println(me)
        }
    }
}