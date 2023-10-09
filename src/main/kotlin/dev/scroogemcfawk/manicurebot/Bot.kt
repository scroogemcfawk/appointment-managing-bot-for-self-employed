package dev.scroogemcfawk.manicurebot

import dev.inmo.micro_utils.common.PreviewFeature
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.modify.setChatPhoto
import dev.inmo.tgbotapi.extensions.api.edit.caption.editMessageCaption
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.buttonField
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.KeyboardButtonRequestChat
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.buttons.reply.simpleReplyButton
import dev.inmo.tgbotapi.types.chat.CommonBot
import dev.inmo.tgbotapi.types.chat.ExtendedBot
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.flatMatrix
import dev.inmo.tgbotapi.utils.row
import io.ktor.server.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.util.*

class Bot(token: String, val locale: Locale) {

    val bot = telegramBot(token)
    val scope = CoroutineScope(Dispatchers.Default)
    val log = LoggerFactory.getLogger(Bot::class.java)

    val dev = ChatId(471271839)


    suspend fun run(): Job {
        return bot.buildBehaviourWithLongPolling(scope) {
            val me = getMe()

            onCommand("start", requireOnlyCommandInMessage = true) {
                sendMessage(it.chat.id, locale.startMessage)
            }

            onCommand("help", requireOnlyCommandInMessage = true) {
                sendMessage(it.chat.id, locale.helpMessage)
            }

            onUnhandledCommand {
                reply(it, locale.unknownCommand)
            }

            onCommand("_id", requireOnlyCommandInMessage = true) {
                sendMessage(
                    dev, "${it.chat.id.chatId} (${it.chat.asPrivateChat()?.firstName} ${
                        it.chat.asPrivateChat()?.lastName
                    } ${it.chat.asPrivateChat()?.username?.username})"
                )
            }

            onCommand("addAppointment") { msg ->
                val today = LocalDate.now()
                sendMessage(msg.chat.id, replyMarkup = InlineKeyboardMarkup(flatMatrix {
                    dataButton(today.month.toString(), "appointment:month:${today.month}")
                    dataButton(
                        today.month.plus(1).toString(), "appointment:month:${
                            today.month.plus(1)
                        }"
                    )
                })) {
                    +"Select month:"
                }
            }

            onDataCallbackQuery {
                var callBackFullfilled = false
                val (source, data) = it.data.split(":", limit = 2)
                when (source) {
                    "appointment" -> {
                        val (dest, value) = data.split(":")
                        when (dest) {
                            "month" -> {
                                log.info("appointment is on ${value}")
                                log.info("${it.message?.messageId}")
                                editMessageText(it.user.id,
                                    it.message!!.messageId, "Select date: ")
                                editMessageReplyMarkup(
                                    it.user.id,
                                    it.message!!.messageId,
                                    replyMarkup = InlineKeyboardMarkup (
                                        flatMatrix(
                                            dataInlineButton("1", "1"),
                                            dataInlineButton("2", "2")
                                        )
                                ))
//                                callBackFullfilled = true
//                                answerCallbackQuery(it)
//                                it.message.messageId
                            }

                            else -> {
                                log.warn("Unrecognized appointment destination \"${dest}\".")
                            }
                        }
                    }

                    else -> {
                        log.warn("Unrecognized callback source \"${source}\".")
                    }
                }
//                if (!callBackFullfilled) {
//                    answerCallbackQuery(it, "Failed to finish operation.")
//                }
                println(source)
                println(data)
            }

            log.info("Bot is initialized and running: ")
            println(me)
        }
    }
}