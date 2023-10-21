package dev.scroogemcfawk.manicurebot.callbacks

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.scroogemcfawk.manicurebot.config.Config
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.*
import dev.scroogemcfawk.manicurebot.keyboards.getInlineCalendarMarkup
import dev.scroogemcfawk.manicurebot.keyboards.getInlineClockMarkup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val log = LoggerFactory.getLogger("CallbackHandler.kt")


@Suppress("SpellCheckingInspection")
class CallbackHandler(
    private val ctx: BehaviourContext,
    private val config: Config,
    private val locale: Locale,
    private val userChats: HashMap<Long, User>,
    private val appointments: AppointmentList
) {

    private val bot = ctx.bot
    private val scope = ctx.scope
    private val contractor = config.manager
    private val notifyBeforeHours = config.notifyClientBeforeAppointmentInHours


    // (',:-|)
    private var add: LocalDate? = null

    private val dateFormat = DateTimeFormatter.ofPattern(locale.dateFormat)
    private val dateTimeFormat = DateTimeFormatter.ofPattern(locale.dateTimeFormat)

    private val log = LoggerFactory.getLogger(this::class.java)!!

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
            log.debug(
                "Suspended notification for {} in {} seconds",
                appointment.datetime,
                timeout / 1000
            )
            delay(timeout)
            bot.send(
                ChatId(id), locale.remindUserMessageTemplate
                    .replace("\$1", appointment.datetime.format(dateTimeFormat))
                    .replace("\$2", locale.address)
            )
        } else {
            log.debug("Timeout is too short.")
        }
    }

    @OptIn(RiskFeature::class)
    @Suppress("LocalVariableName")
    private suspend fun processAdd(cb: DataCallbackQuery, data: String, appointments: AppointmentList) {
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
                    locale.cbAddSelectTimePromptMessage
                )
                this.bot.editMessageReplyMarkup(
                    cb.message!!.chat.id,
                    cb.message!!.messageId,
                    replyMarkup = getInlineClockMarkup()
                )
            }

            "time" -> {
                // TODO: double check if this shit doesn't cause bugs
                val (hs, ms) = value.split(":")
                val Y = add!!.year
                val M = add!!.month
                val D = add!!.dayOfMonth
                val dateTime = restore<LocalDateTime>("Y=$Y:M=$M:D=$D:$hs:$ms")
                this.bot.answerCallbackQuery(cb, "")
                this.bot.editMessageText(
                    cb.message!!.chat.id,
                    cb.message!!.messageId,
                    locale.cbAddTimeAppointmentCreatedTemplate.replace(
                        "\$1",
                        dateTime!!.format(dateTimeFormat)
                    )
                )
                appointments.add(Appointment(dateTime))
                this.bot.editMessageReplyMarkup(
                    cb.message!!.chat.id, cb.message!!.messageId, replyMarkup = null
                )
            }

            else -> {
                answerInvalid(cb)
                log.warn("Invalid signup action: $action")
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
                log.warn("Invalid calendar action: $action")
            }
        }
    }

    @OptIn(RiskFeature::class)
    private suspend fun processAppointment(
        cb: DataCallbackQuery,
        data: String,
        appointments: AppointmentList)
    {
        val (idPair, app) = data.split(":", limit = 2)

        val id = restore<Long>(idPair)!!
        val appointment = restore<Appointment>(app)!!

        if (!appointments.isAvailable(appointment)) {
            bot.editMessageText(
                cb.message!!.chat,
                cb.message!!.messageId,
                locale.appointmentNotAlreadyTakenMessage,
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
        bot.send(
            contractor,
            locale.cbAppointmentCompleteManagerNotificationMessage
                .replace("\$1", appointment.datetime.format(dateFormat))
                .replace("\$2", userChats[cb.user.id.chatId]!!.toString())
        )
    }

    @OptIn(RiskFeature::class)
    private suspend fun processContractorCallback(
        cb: DataCallbackQuery,
        cbData: String,
        appointments: AppointmentList)
    {
        try {
            val (source, data) = cbData.split(":", limit = 2)

            when (source) {
                locale.cancelCommand -> {
                    val appointment = restore<Appointment>(data)
                    appointments.cancel(appointment!!)
                    bot.editMessageText(
                        contractor,
                        cb.message!!.messageId,
                        locale.cancelDoneMessage,
                        replyMarkup = null
                    )
                    bot.answerCallbackQuery(cb)
                }

                else -> {
                    log.warn("Unknown contractor callback source (${source}).")
                }
            }
        } catch (e: Exception) {
            throw Exception("Exception in processContractorCallback: ${e.message}.")
        }
    }

    suspend fun processCallback(cb: DataCallbackQuery, appointments: AppointmentList) {
        try {
            val (source, data) = cb.data.split(":", limit = 2)
            when (source) {
                "empty" -> {
                    answerEmpty(cb)
                }

                // Contractor-specific callbacks
                "c" -> {
                    processContractorCallback(cb, data, appointments)
                }

                locale.addCommand -> {
                    processAdd(cb, data, appointments)
                }

                "calendar" -> {
                    processCalendar(cb, data)
                }

                locale.cancelCommand -> {
                    processCancel(cb, data)
                }

                locale.appointmentCommand -> {
                    processAppointment(cb, data, appointments)
                }

                "L" -> {
                    log.debug("Ignore local callback.")
                }

                else -> {
                    answerInvalid(cb)
                    log.warn("Invalid source: $source")
                }
            }
        } catch (e: Exception) {
            log.error("Faild to process callback: ${e.message}")
        }
    }

    @OptIn(RiskFeature::class)
    suspend fun processCancel(cb: DataCallbackQuery, data: String) {
        // TODO: cancell suspended notification
        val verdct = data.split(":", limit = 2)[0]
        answerEmpty(cb)
        if (verdct == "yes") {
            val appointment = data.split(":", limit = 2)[1]
            restore<Appointment>(appointment)?.run {
                appointments.cancel(this)
            }
            bot.editMessageText(cb.message!!.chat.id, cb.message!!.messageId, locale.cancelDoneMessage, replyMarkup = null)
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
            log.error("Unexpected type found.")
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
