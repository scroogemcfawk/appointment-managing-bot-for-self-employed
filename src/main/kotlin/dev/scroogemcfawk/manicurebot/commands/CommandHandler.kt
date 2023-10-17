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
    private val userChats: HashMap<Long, User>
) {

    private val bot: TelegramBot = ctx.bot
    private val manager = config.manager
    private val dev = config.dev

    private val dateTimeFormat = DateTimeFormatter.ofPattern(locale.dateTimeFormat)

    private val log = LoggerFactory.getLogger(this::class.java)!!

    //=================================== COMMON ===================================================

    suspend fun start(msg: TextMessage) {
        try {
            bot.sendMessage(msg.chat, locale.startMessage)
        } catch (e: Exception) {
            log.error("Error during /${locale.startCommand}")
        }
    }

    suspend fun help(msg: TextMessage) {
        try {
            bot.sendMessage(msg.chat, locale.helpMessage)
        } catch (e: Exception) {
            log.error("Error during /${locale.helpCommand}")
        }
    }

    suspend fun register(
        msg: TextMessage,
        userChats: HashMap<Long, User>,
    ) {
        try {
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
            bot.sendTextMessage(msg.chat.id, locale.registerSuccessfulRegistrationMessageTemplate
                .replace("\$1", locale.appointmentCommand))
        } catch (e: Exception) {
            log.error("Error during /${locale.registerCommand}")
        }
    }

    suspend fun id(
        msg: TextMessage,
    ) {
        try {
            bot.sendMessage(dev, "${msg.chat.id.chatId}")
        } catch (e: Exception) {
            log.error("Error during /${locale.idCommand}")
        }
    }

    suspend fun unhandled(
        msg: TextMessage,
    ) {
        try {
            bot.reply(msg, locale.unknownCommand)
        } catch (e: Exception) {
            log.error("Unhandled command reply failed.")
        }
    }

    //==================================== CLIENT ==================================================

    suspend fun appointment(msg: TextMessage, appointments: ArrayList<Appointment>) {
        try {
            if (msg.chat.id.chatId !in userChats) {
                bot.send(msg.chat.id, locale.appointmentNotRegisteredMessageTemplate
                    .replace("\$1", locale.registerCommand))
                return
            }
            if (!appointments.hasAvailable()) {
                bot.send(msg.chat.id, locale.appointmentAppointmentsNotAvailableMessage)
                return
            }
            appointments.getFutureAppointmentOrNull(msg.chat.id.chatId)
                ?.let { appointment ->
                    bot.send(
                        msg.chat.id, "${locale.appointmentAlreadyHaveAppointmentMessage} ${
                            appointment.datetime.format(
                                dateTimeFormat
                            )
                        }"
                    )
                } ?: run {
                try {
                    bot.send(
                        msg.chat.id,
                        locale.appointmentChooseAppointmentMessage,
                        replyMarkup = getInlineAvailableAppointmentsMarkup(
                            msg.chat.id.chatId,
                            appointments,
                            locale
                        )
                    )
                } catch (e: Exception) {
                    log.warn(e.message)
                }
            }
        } catch (e: Exception) {
            log.error("Error during /${locale.appointmentCommand}")
        }
    }

    //=================================== MANAGER ==================================================

    suspend fun add(msg: TextMessage) {
        try {
            if (msg.chat.id != manager) {
                return
            }
            bot.send(
                msg.chat.id,
                locale.addSelectDayPromptMessage,
                replyMarkup = getInlineCalendarMarkup(YearMonth.now(), locale)
            )
        } catch (e: Exception) {
            log.error("Error during /${locale.addCommand}")
        }
    }

    suspend fun list(msg: TextMessage, appointments: ArrayList<Appointment>) {
        try {
            if (msg.chat.id == manager) {
                bot.sendTextMessage(msg.chat.id,
                    appointments.joinToString(",\n") { it.datetime.format(dateTimeFormat) })
            }
        } catch (e: Exception) {
            log.error("Error during /${locale.listCommand}")
        }
    }

    suspend fun notify(msg: TextMessage, users: HashMap<Long, User>) {
        try {
            if (msg.chat.id == manager) {
                val text =
                    ctx.waitText(SendTextMessage(msg.chat.id, locale.notifyNotificationMessagePromptMessage)).first().text
                for ((id, _) in users.toList()) {
                    bot.send(ChatId(id), text)
                }
            }
        } catch (e: Exception) {
            log.error("Error during /${locale.notifyCommand}")
        }
    }
}
