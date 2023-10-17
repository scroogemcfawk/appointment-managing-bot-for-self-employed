package dev.scroogemcfawk.manicurebot.callbacks

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
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
import dev.scroogemcfawk.manicurebot.domain.Appointment
import dev.scroogemcfawk.manicurebot.domain.User
import dev.scroogemcfawk.manicurebot.domain.isStillAvailable
import dev.scroogemcfawk.manicurebot.domain.occupy
import dev.scroogemcfawk.manicurebot.keyboards.getInlineCalendarMarkup
import dev.scroogemcfawk.manicurebot.keyboards.getInlineClockMarkup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("CallbackHandler.kt")


@Suppress("SpellCheckingInspection")
class CallbackHandler(
    private val ctx: BehaviourContext,
    private val config: Config,
    private val locale: Locale,
    private val appointments: ArrayList<Appointment>,
    private val userChats: HashMap<Long, User>
) {

    private val bot = ctx.bot
    private val scope = ctx.scope
    private val manager = config.manager
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
            (dif - TimeUnit.HOURS.toMillis(notifyBeforeHours.toLong())).run {
                log.info("Suspended notification for ${appointment.datetime} in $this")
                delay(this)
            }
            bot.send(
                ChatId(id), locale.remindUserMessageTemplate
                    .replace("\$1", appointment.datetime.format(dateTimeFormat))
                    .replace("\$2", locale.address)
            )
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
    private suspend fun processAppointment(cb: DataCallbackQuery, data: String) {
        val (idPair, app) = data.split(":", limit = 2)

        val id = restore<Long>(idPair)!!
        val appointment = restore<Appointment>(app)!!

        if (!appointments.isStillAvailable(appointment)) {
            bot.editMessageText(
                cb.message!!.chat,
                cb.message!!.messageId,
                locale.appointmentNotAlreadyTakenMessage,
                replyMarkup = null
            )
            answerEmpty(cb)
            return
        }

        appointments.occupy(appointment, id)
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
            manager,
            locale.cbAppointmentCompleteManagerNotificationMessage
                .replace("\$1", appointment.datetime.format(dateFormat))
                .replace("\$2", userChats[cb.user.id.chatId]!!.toString())
        )
    }

    suspend fun processCallback(cb: DataCallbackQuery) {
        try {
            val (source, data) = cb.data.split(":", limit = 2)
            when (source) {
                "empty" -> {
                    answerEmpty(cb)
                }

                locale.addCommand -> {
                    processAdd(cb, data)
                }

                "calendar" -> {
                    processCalendar(cb, data)
                }

                locale.appointmentCommand -> {
                    processAppointment(cb, data)
                }

                else -> {
                    answerInvalid(cb)
                    log.warn("Invalid source: $source")
                }
            }
        } catch (e: Exception) {
            log.error("Faild to process callback $cb")
        }
    }
}

private fun HashMap<String, String>.fromCallbackArgs(args: List<String>): HashMap<String, String> {
    for (p in args) {
        val (k, v) = p.split("=")
        this[k] = v
    }
    return this
}

private inline fun <reified T: Any?> restore(s: String): T? {
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

private fun restoreLocalDate(map: HashMap<String, String>): LocalDate {
    return LocalDate.of(map["Y"]!!.toInt(), Month.valueOf(map["M"]!!), map["D"]!!.toInt())
}

private fun restoreLocalTime(map: HashMap<String, String>): LocalTime {
    return LocalTime.of(map["h"]!!.toInt(), map["m"]!!.toInt())
}
