package smf.samurai1.callbacks

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCallbackQueries
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.requests.edit.text.EditChatMessageText
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import smf.samurai1.chatId
import smf.samurai1.config.Config
import smf.samurai1.config.Locale
import smf.samurai1.domain.*
import smf.samurai1.keyboards.getInlineCalendarMarkup
import smf.samurai1.keyboards.getInlineClockMarkup
import smf.samurai1.keyboards.getRescheduleMarkupInline
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.tinylog.Logger
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@Suppress("SpellCheckingInspection")
class CallbackHandler(
    private val ctx: BehaviourContext,
    config: Config,
    private val locale: Locale,
    private val clientChats: ClientRepo,
    private val appointments: AppointmentRepo
) {

    private val bot = ctx.bot
    private val scope = ctx.scope
    private val contractor = config.manager
    private val notifyBeforeHours = config.notifyClientBeforeAppointmentInHours


    // (',:-|)
    private var add: LocalDate? = null

    private val dateFormat = DateTimeFormatter.ofPattern(locale.dateFormat)
    private val dateTimeFormat = DateTimeFormatter.ofPattern(locale.dateTimeFormat)

    private suspend fun answerEmpty(cb: DataCallbackQuery) {
        bot.answerCallbackQuery(cb, "")
    }

    private suspend fun answerInvalid(cb: DataCallbackQuery) {
        bot.answerCallbackQuery(cb, locale.cbInvalidCallbackNotificationMessage)
    }

    private suspend fun remindUser(id: Long, appointment: Appointment) {
        val dif = ChronoUnit.HOURS.between(LocalDateTime.now(), appointment.datetime)
        if (dif >= notifyBeforeHours) {
            val timeout = TimeUnit.HOURS.toMillis(dif - notifyBeforeHours.toLong())
            Logger.debug{ "Suspended notification for ${appointment.datetime} in ${timeout / 1000} seconds" }
            delay(timeout)
            bot.send(
                ChatId(id), locale.remindUserMessageTemplate
                    .replace("\$1", appointment.datetime.format(dateTimeFormat))
                    .replace("\$2", locale.address)
            )
        } else {
            Logger.debug { "Timeout is too short." }
        }
    }

    @OptIn(RiskFeature::class)
    @Suppress("LocalVariableName")
    private suspend fun processAdd(cb: DataCallbackQuery, data: String) {
        val (action, value) = data.split(":", limit = 2)
        when (action) {
            "select" -> {
                val (ys, ms, ds) = value.split(":")
                val y = ys.split("=")[1].toInt()
                val m = Month.valueOf(ms.split("=")[1])
                val d = ds.split("=")[1].toInt()
                this.bot.answerCallbackQuery(cb, "")
                add = LocalDate.of(y, m, d)
                this.bot.editMessageText(
                    cb.message!!.chat.id,
                    cb.message!!.messageId,
                    locale.cbAddSelectTimePromptMessage,
                    replyMarkup = getInlineClockMarkup()
                )
            }

            "time" -> {
                // TODO: double check if this shit doesn't cause bugs
                val (hs, ms) = value.split(":")
                val Y = add!!.year
                val M = add!!.month
                val D = add!!.dayOfMonth
                val dateTime = try {
                    restore<LocalDateTime>("Y=$Y:M=$M:D=$D:$hs:$ms")!!
                } catch (_: Exception) {
                    throw Exception("Unable to restore Appointment at processAdd()")
                }
                this.bot.answerCallbackQuery(cb, "")
                appointments.add(Appointment(dateTime))
                this.bot.editMessageText(
                    cb.message!!.chat.id,
                    cb.message!!.messageId,
                    locale.cbAddTimeAppointmentCreatedTemplate.replace(
                        "\$1",
                        dateTime.format(dateTimeFormat)
                    ),
                    replyMarkup = null
                )
            }

            else -> {
                answerInvalid(cb)
                Logger.warn{ "Invalid signup action: $action" }
            }
        }
    }

    @OptIn(RiskFeature::class)
    private suspend fun processCalendar(cb: DataCallbackQuery, data: String) {
        val (action, value) = data.split(":", limit = 2)
        val (yearArg, monthArg) = value.split(":", limit = 2)

        val year = Year.of(yearArg.split("=")[1].toInt())
        val month = Month.valueOf(monthArg.split("=")[1])
        when (action) {
            "prevMonth" -> {
                val prev = YearMonth.of(year.value, month).minusMonths(1)
                this.bot.answerCallbackQuery(cb, "${locale.cbCalendarSwitchingNotificationMessage} $prev")
                this.bot.editMessageReplyMarkup(
                    cb.message!!.chat.id,
                    cb.message!!.messageId,
                    replyMarkup = getInlineCalendarMarkup(prev, locale)
                )
            }

            "nextMonth" -> {
                val next = YearMonth.of(year.value, month).plusMonths(1)
                this.bot.answerCallbackQuery(cb, "${locale.cbCalendarSwitchingNotificationMessage} $next")
                this.bot.editMessageReplyMarkup(
                    cb.message!!.chat.id,
                    cb.message!!.messageId,
                    replyMarkup = getInlineCalendarMarkup(next, locale)
                )
            }

            else -> {
                answerInvalid(cb)
                Logger.warn{ "Invalid calendar action: $action" }
            }
        }
    }

    @OptIn(RiskFeature::class)
    private suspend fun processAppointment(cb: DataCallbackQuery, data: String) {
        if (data == "cancel") {
            bot.answerCallbackQuery(cb)
            bot.edit(
                cb.user.id,
                cb.message!!.messageId,
                locale.operationCancelledMessage,
                replyMarkup = null
            )
            return
        }

        val (idPair, app) = data.split(":", limit = 2)

        val id = restore<Long>(idPair)!!
        val appointment = restore<Appointment>(app)!!

        if (!appointments.isAvailable(appointment)) {
            bot.editMessageText(
                cb.message!!.chat,
                cb.message!!.messageId,
                locale.appointmentAlreadyTakenMessage,
                replyMarkup = null
            )
            answerEmpty(cb)
            return
        }

        appointments.assignClient(appointment, id)
        scope.launch {
            remindUser(id, appointment)
        }
        answerEmpty(cb)
        bot.editMessageText(
            cb.message!!.chat,
            cb.message!!.messageId,
            locale.cbAppointmentCompleteMessage
                .replace("\$1", appointment.datetime.format(dateTimeFormat)),
            replyMarkup = null
        )
        if (contractor != null) {
            bot.send(
                contractor.chatId,
                locale.cbAppointmentCompleteManagerNotificationMessage
                        .replace("\$1", appointment.datetime.format(dateFormat))
                        .replace("\$2", clientChats[cb.user.id.chatId]!!.toString())
            )
        }
    }

    @Throws(Exception::class)
    @OptIn(RiskFeature::class)
    private suspend fun processContractorCallback(cb: DataCallbackQuery, cbData: String) {
        try {
            val (source, data) = cbData.split(":", limit = 2)
            when (source) {
                locale.cancelCommand -> {
                    val appointment = restore<Appointment>(data)
                    appointments.cancel(appointment!!)
                    if (contractor != null) {
                        bot.editMessageText(
                            contractor.chatId,
                            cb.message!!.messageId,
                            locale.cancelDoneMessage,
                            replyMarkup = null
                        )
                        bot.answerCallbackQuery(cb)
                        if (appointment.client != contractor) {
                            appointment.client?.let { notify(it, locale.appointmentHasBeenCanceledMessage) }
                        }
                    }
                }

                locale.rescheduleCommandShort -> {
                    val (action, value) = data.split(":", limit = 2)
                    when (action) {
                        locale.rescheduleContractorShowAppointmentsToRescheduleToAction -> {
                            bot.answerCallbackQuery(cb)

                            val old = restore<Appointment>(value)

                            val localCallback = ctx.waitCallbackQueries<DataCallbackQuery>(
                                EditChatMessageText(
                                    cb.user.id,
                                    cb.message!!.messageId,
                                    locale.rescheduleChooseAppointmentToRescheduleToPromptMessage,
                                    replyMarkup = getRescheduleMarkupInline(
                                        cb.user.id.chatId,
                                        appointments,
                                        locale
                                    )
                                )
                            ).first().data
                            val new = restore<Appointment>(
                                localCallback.split(":", limit = 2)[1]
                            )

                            bot.answerCallbackQuery(cb)
                            appointments.reschedule(old!!, new!!)

                            bot.editMessageText(
                                cb.user.id,
                                cb.message!!.messageId,
                                locale.rescheduleDoneMessageTemplate
                                    .replace("\$1", old.datetime.format(dateTimeFormat))
                                    .replace("\$2", new.datetime.format(dateTimeFormat)),
                                replyMarkup = null
                            )
                        }
                        else -> {
                            throw Exception("Reschedule unknown action (${action}).")
                        }
                    }
                }

                locale.deleteCommandShort -> {
                    bot.answerCallbackQuery(cb)
                    val appointment = restore<Appointment>(data)
                    appointments.delete(appointment!!)
                    if (contractor != null) {
                        bot.edit(
                            contractor.chatId,
                            cb.message!!.messageId,
                            locale.deleteSuccessMessage,
                            replyMarkup = null
                        )
                        if (appointment.client != contractor) {
                            appointment.client?.let { notify(it, locale.appointmentHasBeenCanceledMessage) }
                        }
                    }
                }

                else -> {
                    bot.answerCallbackQuery(cb, "Unknown callback.")
                    Logger.warn{"Unknown contractor callback source (${source})."}
                }
            }
        } catch (e: Exception) {
            throw Exception("Exception in processContractorCallback: $e.")
        }
    }

    private suspend fun notify(client: Long, message: String) {
        try {
            bot.send(
                ChatId(client),
                message
            )
        } catch (e: Exception) {
            Logger.error{"Failed to notify user: ${e.message}"}
        }
    }

    suspend fun processCallback(cb: DataCallbackQuery) {
        try {
            val (source, data) = cb.data.split(":", limit = 2)
            Logger.debug{"Source: $source"}
            Logger.debug{"Data: $data"}
            when (source) {
                "empty" -> {
                    answerEmpty(cb)
                }

                // Contractor-specific callbacks
                "c" -> {
                    processContractorCallback(cb, data)
                }

                locale.addCommand -> {
                    processAdd(cb, data)
                }

                "calendar" -> {
                    processCalendar(cb, data)
                }

                locale.cancelCommand -> {
                    processCancel(cb, data)
                }

                locale.appointmentCommand -> {
                    processAppointment(cb, data)
                }

                "L" -> {
                    Logger.debug{"Ignore local callback."}
                }

                else -> {
                    answerInvalid(cb)
                    Logger.warn{"Invalid source: $source"}
                }
            }
        } catch (e: Exception) {
            Logger.error{"Faild to process callback: ${e.message}"}
        }
    }

    @OptIn(RiskFeature::class)
    suspend fun processCancel(cb: DataCallbackQuery, data: String) {
        // TODO: cancell suspended notification
        val verdct = data.split(":", limit = 2)[0]
        answerEmpty(cb)
        if (verdct == "yes") {
            val appointment = restore<Appointment>(data.split(":", limit = 2)[1]) ?: throw Exception("Failed to restore appointment.")
            appointment.run {
                appointments.cancel(this)
            }
            bot.editMessageText(cb.message!!.chat.id, cb.message!!.messageId, locale.cancelDoneMessage, replyMarkup = null)
            if (contractor != null) {
                bot.send(
                    contractor.chatId,
                    locale.appointmentHasBeenCanceledTemplate.replace("\$1", appointment.datetime.format(dateTimeFormat))
                )
            }
        } else {
            bot.delete(cb.message!!.chat.id, cb.message!!.messageId)
        }
    }
}

fun HashMap<String, String>.fromCallbackArgs(args: List<String>): HashMap<String, String> {
    for (p in args) {
        val (k, v) = p.split("=")
        this[k] = v
    }
    return this
}

inline fun <reified T: Any?> restore(s: String): T? {
    val args = s.split(":")
    val map = HashMap<String, String>().fromCallbackArgs(args)

    when (T::class) {
        Long::class -> {
            return s.split("=")[1].toLong() as T
        }

        Appointment::class -> {

            val ld = restoreLocalDate(map)
            val lt = restoreLocalTime(map)
            val id = map["id"]!!
            return Appointment(
                LocalDateTime.of(ld, lt),
                if (id == "null") null else id.toLong()
            ) as T
        }

        LocalDateTime::class -> {
            return LocalDateTime.of(restoreLocalDate(map), restoreLocalTime(map)) as T
        }

        else -> {
            Logger.error{ "Unexpected type found." }
            return null
        }
    }
}

fun restoreLocalDate(map: HashMap<String, String>): LocalDate {
    return LocalDate.of(map["Y"]!!.toInt(), Month.valueOf(map["M"]!!), map["D"]!!.toInt())
}

fun restoreLocalTime(map: HashMap<String, String>): LocalTime {
    return LocalTime.of(map["h"]!!.toInt(), map["m"]!!.toInt())
}
