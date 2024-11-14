package smf.samurai1.commands

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
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import smf.samurai1.callbacks.restore
import smf.samurai1.chatId
import smf.samurai1.config.Config
import smf.samurai1.config.Locale
import smf.samurai1.domain.*
import smf.samurai1.keyboards.*
import kotlinx.coroutines.flow.first
import org.tinylog.Logger
import java.time.YearMonth
import java.time.format.DateTimeFormatter

typealias TextMessage = CommonMessage<TextContent>

class CommandHandler(
    private val ctx: BehaviourContext,
    config: Config,
    private val locale: Locale,
    private val clientChats: ClientRepo,
    private val appointments: AppointmentRepo
) {

    private val bot: TelegramBot = ctx.bot
    private val contractor = config.manager
    private val dev = config.dev

    @Suppress("unused")
    private val callbackSessions = CallbackSessions()

    private val dateTimeFormat = DateTimeFormatter.ofPattern(locale.dateTimeFormat)

    //================================== COMMAND SECTION ===========================================
    //====================================== COMMON ================================================

    suspend fun start(msg: TextMessage) {
        try {
            bot.sendMessage(msg.chat, locale.startMessageTemplate.replace("\$1", locale.registerCommand))
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.startCommand} : ${e.message}"}
        }
    }

    suspend fun help(msg: TextMessage) {
        try {
            if (msg.chat.id != contractor?.chatId) {
                bot.sendMessage(msg.chat, locale.helpMessage)
            } else {
                bot.sendMessage(msg.chat, locale.helpContractorMessage)
            }
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.helpCommand} : ${e.message}"}
        }
    }

    suspend fun register(msg: TextMessage) {
        try {
            if (msg.chat.id.chatId in clientChats) {
                bot.sendTextMessage(msg.chat.id, locale.registerUserAlreadyExistsMessage)
                return
            }

            val name = ctx.waitText(
                SendTextMessage(msg.chat.id, locale.registerUserNamePromptMessage)
            ).first().text

            val phone = ctx.waitText(
                SendTextMessage(msg.chat.id, locale.registerUserPhonePromptMessage)
            ).first().text

            clientChats[msg.chat.id.chatId] = Client(msg.chat.id.chatId, name, phone)

            bot.sendTextMessage(
                msg.chat.id,
                locale.registerSuccessfulRegistrationMessage + "\n" + locale.helpMessage
            )
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.registerCommand} : ${e.message}"}
        }
    }

    fun id(msg: TextMessage) {
        Logger.info{ "${msg.from?.username?.username}: " + msg.chat.id.chatId.toString()}
    }

    suspend fun unhandled(msg: TextMessage) {
        try {
            bot.reply(msg, locale.unknownCommand)
        } catch (e: Exception) {
            Logger.error{"Error on unhandled command reply."}
        }
    }

    //==================================== CLIENT ==================================================

    suspend fun appointment(msg: TextMessage) {
        try {
            if (!appointments.hasAvailable()) {
                bot.send(msg.chat.id, locale.appointmentAppointmentsNotAvailableMessage)
                return
            }
            appointments.clearOld()
            if (msg.chat.id == contractor?.chatId) {
                makeAppointmentAsContractor(appointments)
            } else {
                makeAppointmentAsClient(msg)
            }
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.appointmentCommand} : ${e.message}"}
        }
    }

    @OptIn(RiskFeature::class)
    suspend fun reschedule(msg: CommonMessage<TextContent>) {
        try {
            if (!appointments.hasAvailable()) {
                bot.send(
                    msg.chat,
                    locale.rescheduleNoAppointmentsAvailableMessage
                )
                return
            }
            if (msg.chat.id == contractor?.chatId) {
                bot.send(
                    contractor.chatId,
                    locale.rescheduleContractorChooseAppointmentToReschedulePromptMessage,
                    replyMarkup = getAppointmentListInlineMarkup(
                        contractor,
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
                    if (contractor != null) {
                        bot.send(
                            contractor.chatId,
                            locale.appointmentRescheduledNotificationTemplate
                                    .replace("\$1", oldAppointment.datetime.format(dateTimeFormat))
                                    .replace("\$2", newAppointment.datetime.format(dateTimeFormat))
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.rescheduleCommand} : ${e.message}"}
        }
    }


    suspend fun cancel(msg: TextMessage) {
        try {
            if (!appointments.clientHasAppointment(msg.chat.id.chatId)) {
                bot.send(msg.chat.id, locale.cancelNoAppointmentsFoundMessageTemplate.replace("\$1", locale.appointmentCommand))
                return
            }
            if (msg.chat.id == contractor?.chatId) {
                cancelAsContractor(msg)
            } else {
                cancelAsClient(msg)
            }
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.cancelCommand} : ${e.message}"}
        }
    }

    //=================================== CONTRACTOR ===============================================

    suspend fun add(msg: TextMessage) {
        try {
            if (msg.chat.id != contractor?.chatId) {
                return
            }
            bot.send(
                msg.chat.id,
                locale.addSelectDayPromptMessage,
                replyMarkup = getInlineCalendarMarkup(YearMonth.now(), locale)
            )
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.addCommand} : ${e.message}"}
        }
    }

    suspend fun list(msg: TextMessage) {
        try {
            if (msg.chat.id == contractor?.chatId) {
                ArrayList<String>().joinToString("") { it.length.toString() }
                bot.sendTextMessage(
                    msg.chat.id,
                    appointments.allFuture.sortedBy { it.datetime }.run{
                        if (this.isNotEmpty()) this.joinToString("\n\n") { app ->
//                            app.datetime.format(dateTimeFormat) + " " + (app.client?.let {
//                                "${clientChats[it]?.name ?: locale.available} ${clientChats[it]?.phoneNumber ?: ""}"
//                            } ?: locale.available)
                            app.datetime.format(dateTimeFormat) + " " +(app.client?.let { clientChats[it] } ?: locale.available)
                        }
                        else locale.listNoAppointmentsMessage
                    },
                )
            }
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.listCommand} : ${e.message}"}
        }
    }

    suspend fun delete(msg: TextMessage) {
        try {
            if (msg.chat.id == contractor?.chatId) {
                bot.send(
                    contractor.chatId,
                    locale.deleteSelectAppointmentPromptMessage,
                    replyMarkup = getAppointmentListInlineMarkup(
                        contractor,
                        appointments,
                        dateTimeFormat,
                        "c:${locale.deleteCommandShort}",
                        filter = false
                    )
                )
            }
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.deleteCommand} : ${e.message}"}
        }
    }

    suspend fun notify(msg: TextMessage) {
        try {
            if (msg.chat.id == contractor?.chatId) {
                val text =
                    ctx.waitText(
                        SendTextMessage(
                            msg.chat.id,
                            locale.notifyNotificationMessagePromptMessage
                        )
                    ).first().text
                for ((id, _) in clientChats.toList()) {
                    bot.send(ChatId(id), text)
                }
            }
        } catch (e: Exception) {
            Logger.error{"Error on /${locale.notifyCommand} : ${e.message}"}
        }
    }

    //=============================== END OF COMMAND SECTION =======================================
    //=============================== HELPER METHOD SECTION ========================================

    private suspend fun makeAppointmentAsClient(msg: TextMessage) {
        if (msg.chatId !in clientChats) {
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

    private suspend fun makeAppointmentAsContractor(appointments: AppointmentRepo) {
        if (contractor != null) {
            bot.send(
                contractor.chatId,
                locale.appointmentChooseAppointmentMessage,
                replyMarkup = getInlineAvailableAppointmentsMarkup(
                    contractor,
                    appointments,
                    locale
                )
            )
        }
    }

    private suspend fun cancelAsClient(msg: TextMessage) {
        appointments.getClientAppointmentOrNull(msg.chat.id.chatId)?.run {
            bot.send(
                msg.chat,
                locale.cancelConfirmMessageTemplate
                    .replace("\$1", this.datetime.format(dateTimeFormat)),
                replyMarkup = getYesNoInlineMarkup(this)
            )
        } ?: Logger.error{"CommandHandler.cancelAsClient(): Appointment not found."}
    }

    private suspend fun cancelAsContractor(msg: TextMessage) {
        bot.send(
            msg.chat,
            locale.cancelContractorChoosePromptMessage,
            replyMarkup = getContractorCancelInline(msg.chatId, appointments, dateTimeFormat, locale)
        )
    }
}
