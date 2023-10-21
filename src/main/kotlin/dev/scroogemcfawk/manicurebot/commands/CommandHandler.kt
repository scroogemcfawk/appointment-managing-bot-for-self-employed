package dev.scroogemcfawk.manicurebot.commands

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCallbackQueries
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.scroogemcfawk.manicurebot.callbacks.restore
import dev.scroogemcfawk.manicurebot.chatId
import dev.scroogemcfawk.manicurebot.config.Config
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.Appointment
import dev.scroogemcfawk.manicurebot.domain.AppointmentList
import dev.scroogemcfawk.manicurebot.domain.CallbackSessions
import dev.scroogemcfawk.manicurebot.domain.User
import dev.scroogemcfawk.manicurebot.keyboards.*
import kotlinx.coroutines.flow.first
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.time.format.DateTimeFormatter

typealias TextMessage = CommonMessage<TextContent>

class CommandHandler(
    private val ctx: BehaviourContext,
    config: Config,
    private val locale: Locale,
    private val userChats: HashMap<Long, User>,
) {

    private val bot: TelegramBot = ctx.bot
    private val contractor = config.manager
    private val dev = config.dev

    @Suppress("unused")
    private val callbackSessions = CallbackSessions()

    private val dateTimeFormat = DateTimeFormatter.ofPattern(locale.dateTimeFormat)

    private val log = LoggerFactory.getLogger(this::class.java)!!

    //================================== COMMAND SECTION ===========================================
    //====================================== COMMON ================================================

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

            val name = ctx.waitText(
                SendTextMessage(msg.chat.id, locale.registerUserNamePromptMessage)
            ).first().text

            val phone = ctx.waitText(
                SendTextMessage(msg.chat.id, locale.registerUserPhonePromptMessage)
            ).first().text

            userChats[msg.chat.id.chatId] = User(msg.chat.id.chatId, name, phone)

            bot.sendTextMessage(
                msg.chat.id,
                locale.registerSuccessfulRegistrationMessageTemplate
                    .replace("\$1", locale.appointmentCommand)
            )
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

    suspend fun appointment(msg: TextMessage, appointments: AppointmentList) {
        try {
            if (!appointments.hasAvailable()) {
                bot.send(msg.chat.id, locale.appointmentAppointmentsNotAvailableMessage)
                return
            }
            if (msg.chat.id == contractor) {
                makeAppointmentAsContractor(appointments)
            } else {
                makeAppointmentAsClient(msg, appointments)
            }
        } catch (e: Exception) {
            log.error("Error during /${locale.appointmentCommand}")
        }
    }

    @OptIn(RiskFeature::class)
    suspend fun reschedule(msg: CommonMessage<TextContent>, appointments: AppointmentList) {
        try {
            if (!appointments.hasAvailable()) {
                bot.send(
                    msg.chat,
                    locale.rescheduleNoAppointmentsAvailableMessage
                )
                return
            }
            if (msg.chat.id == contractor) {
                bot.send(
                    contractor,
                    locale.rescheduleContractorChooseAppointmentToReschedulePromptMessage,
                    replyMarkup = getAppointmentListInlineMarkup(
                        contractor.chatId,
                        appointments,
                        dateTimeFormat,
                        "c:${locale.rescheduleCommandShort}:${locale
                            .rescheduleContractorShowAppointmentsToRescheduleToAction}"
                    )
                )
            } else {
                val oldAppointment = appointments.getClientAppointmentOrNull(msg.chatId)
                if (oldAppointment == null) {
                    bot.send(msg.chat, locale.rescheduleYouDontHaveAppointmentMessage)
                    return
                }
                // TODO: move to callback handler
                val cb = ctx.waitCallbackQueries<DataCallbackQuery>(
                    SendTextMessage(
                        msg.chat.id,
                        locale.rescheduleChooseAppointmentToRescheduleToPromptMessage,
                        replyMarkup = getRescheduleMarkupInline(
                            msg.chatId,
                            appointments,
                            locale
                        )
                    )
                ).first()
                bot.answerCallbackQuery(cb)
                val newAppointment = restore<Appointment>(cb.data.split(":", limit = 2)[1])!!
                if (appointments.reschedule(oldAppointment, newAppointment)) {
                    bot.editMessageText(
                        cb.message!!.chat.id,
                        cb.message!!.messageId,
                        locale.rescheduleDoneMessageTemplate
                            .replace("\$1", oldAppointment.datetime.format(dateTimeFormat))
                            .replace("\$2", newAppointment.datetime.format(dateTimeFormat)),
                        replyMarkup = null
                    )
                }
            }
        } catch (e: Exception) {
            log.error("Error during /${locale.rescheduleCommand} ${e.message}")
        }
    }


    suspend fun cancel(msg: TextMessage, appointments: AppointmentList) {
        try {
            if (!appointments.clientHasAppointment(msg.chat.id.chatId)) {
                bot.send(msg.chat.id, locale.cancelNoAppointmentsFoundMessage)
                return
            }
            if (msg.chat.id == contractor) {
                cancelAsContractor(msg, appointments)
            } else {
                cancelAsClient(msg, appointments)
            }
        } catch (e: Exception) {
            log.error("Error during /${locale.cancelCommand}")
        }
    }

    //=================================== CONTRACTOR ===============================================

    suspend fun add(msg: TextMessage) {
        try {
            if (msg.chat.id != contractor) {
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

    suspend fun list(msg: TextMessage, appointments: AppointmentList) {
        try {
            if (msg.chat.id == contractor) {
                ArrayList<String>().joinToString("") { it.length.toString() }
                bot.sendTextMessage(msg.chat.id,
                    appointments.joinToString(",\n") { it.datetime.format(dateTimeFormat) })
            }
        } catch (e: Exception) {
            log.error("Error during /${locale.listCommand}")
        }
    }

    suspend fun notify(msg: TextMessage, users: HashMap<Long, User>) {
        try {
            if (msg.chat.id == contractor) {
                val text =
                    ctx.waitText(
                        SendTextMessage(
                            msg.chat.id,
                            locale.notifyNotificationMessagePromptMessage
                        )
                    ).first().text
                for ((id, _) in users.toList()) {
                    bot.send(ChatId(id), text)
                }
            }
        } catch (e: Exception) {
            log.error("Error during /${locale.notifyCommand}")
        }
    }

    //=============================== END OF COMMAND SECTION =======================================
    //=============================== HELPER METHOD SECTION ========================================

    private suspend fun makeAppointmentAsClient(msg: TextMessage, appointments: AppointmentList) {
        if (msg.chatId !in userChats) {
            bot.send(
                msg.chat.id, locale.appointmentNotRegisteredMessageTemplate
                    .replace("\$1", locale.registerCommand)
            )
            return
        }
        appointments.getFutureAppointmentOrNull(msg.chatId)?.let { appointment ->
            bot.send(
                msg.chat.id, "${locale.appointmentAlreadyHaveAppointmentMessage} ${
                    appointment.datetime.format(
                        dateTimeFormat
                    )
                }"
            )
        } ?: run {
            bot.send(
                msg.chat.id,
                locale.appointmentChooseAppointmentMessage,
                replyMarkup = getInlineAvailableAppointmentsMarkup(
                    msg.chat.id.chatId,
                    appointments,
                    locale
                )
            )
        }
    }

    private suspend fun makeAppointmentAsContractor(appointments: AppointmentList) {
        bot.send(
            contractor,
            locale.appointmentChooseAppointmentMessage,
            replyMarkup = getInlineAvailableAppointmentsMarkup(
                contractor.chatId,
                appointments,
                locale
            )
        )
    }

    private suspend fun cancelAsClient(msg: TextMessage, appointments: AppointmentList) {
        appointments.getClientAppointmentOrNull(msg.chat.id.chatId)?.run {
            bot.send(
                msg.chat,
                locale.cancelConfirmMessageTemplate
                    .replace("\$1", this.datetime.format(dateTimeFormat)),
                replyMarkup = getYesNoInlineMarkup(this)
            )
        } ?: log.error("Appointment not found.")
    }

    private suspend fun cancelAsContractor(msg: TextMessage, appointments: AppointmentList) {
        bot.send(
            msg.chat,
            locale.cancelContractorChoosePromptMessage,
            replyMarkup = getContractorCancelInline(msg.chatId, appointments, dateTimeFormat, locale)
        )
    }
}
