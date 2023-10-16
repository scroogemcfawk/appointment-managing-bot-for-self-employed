package dev.scroogemcfawk.manicurebot.commands

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.scroogemcfawk.manicurebot.config.Config
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.Appointment
import dev.scroogemcfawk.manicurebot.domain.User
import dev.scroogemcfawk.manicurebot.domain.getFutureAppointmentOrNull
import dev.scroogemcfawk.manicurebot.domain.hasAvailable
import dev.scroogemcfawk.manicurebot.keyboards.getInlineAvailableAppointmentsMarkup
import dev.scroogemcfawk.manicurebot.keyboards.getInlineCalendarMarkup
import kotlinx.coroutines.flow.first
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.time.format.DateTimeFormatter

typealias TextMessage = CommonMessage<TextContent>

class CommandHandler(
    private val ctx: BehaviourContext,
    config: Config,
    private val locale: Locale,
) {

    private val bot: TelegramBot = ctx.bot
    private val manager = config.manager

//    private val dateFormat = DateTimeFormatter.ofPattern(locale.dateFormat)
    private val dateTimeFormat = DateTimeFormatter.ofPattern(locale.dateTimeFormat)

    private val log = LoggerFactory.getLogger(this::class.java)!!

    //=================================== COMMON ===================================================

    suspend fun start(msg: TextMessage) {
        bot.sendMessage(msg.chat, locale.startMessage)
    }

    suspend fun help(msg: TextMessage) {
        bot.sendMessage(msg.chat, locale.helpMessage)
    }

    suspend fun register(
        msg: TextMessage,
        userChats: HashMap<Long, User>,
    ) {
        if (msg.chat.id.chatId in userChats) {
            bot.sendTextMessage(msg.chat.id, locale.registerUserAlreadyExistsMessage)
            return
        }
        val name = ctx.waitText(SendTextMessage(msg.chat.id, locale.registerUserNamePromptMessage))
            .first().text
        val phone =
            ctx.waitText(SendTextMessage(msg.chat.id, locale.registerUserPhonePromptMessage))
                .first().text
        userChats[msg.chat.id.chatId] = User(msg.chat.id.chatId, name, phone)
        bot.sendTextMessage(msg.chat.id, locale.registerSuccessfulRegistrationMessage)
    }

    suspend fun id(
        msg: TextMessage,
        dev: ChatId,
    ) {
        bot.sendMessage(dev, "${msg.chat.id.chatId}")
    }

    suspend fun unhandled(
        msg: TextMessage,
        unknownCommandMessage: String,
    ) {
        bot.reply(msg, unknownCommandMessage)
    }

    //==================================== CLIENT ==================================================

    suspend fun signup(msg: TextMessage, appointments: ArrayList<Appointment>) {
        if (appointments.hasAvailable()) {
            appointments.getFutureAppointmentOrNull(msg.chat.id.chatId)
                ?.let { appointment ->
                    bot.send(
                        msg.chat.id, "You already have an appointment: ${
                            appointment.datetime.format(
                                dateTimeFormat
                            )
                        }"
                    )
                } ?: run {
                try {
                    bot.send(
                        msg.chat.id,
                        "Choose an appointment:",
                        replyMarkup = getInlineAvailableAppointmentsMarkup(msg.chat.id.chatId, appointments)
                    )
                } catch (e: Exception) {
                    log.warn("On send: " + e.message)
                }
            }
        } else {
            bot.send(msg.chat.id, "Appointments are not available.")
        }
    }

    //=================================== MANAGER ==================================================

    suspend fun add(msg: TextMessage) {
        if (msg.chat.id != manager) {
            return
        }
        bot.send(
            msg.chat.id,
            "Select day:",
            replyMarkup = getInlineCalendarMarkup(YearMonth.now(), locale)
        )
    }

    suspend fun list(msg: TextMessage, appointments: ArrayList<Appointment>) {
        if (msg.chat.id == manager) {
            bot.sendTextMessage(msg.chat.id,
                appointments.joinToString(",\n") { it.datetime.format(dateTimeFormat) })
        }
    }

    suspend fun notify(msg: TextMessage, users: HashMap<Long, User>) {
        if (msg.chat.id == manager) {
            val text =
                ctx.waitText(SendTextMessage(msg.chat.id, "Notification message:")).first().text
            for ((id, _) in users.toList()) {
                bot.send(ChatId(id), text)
            }
        }
    }
}








