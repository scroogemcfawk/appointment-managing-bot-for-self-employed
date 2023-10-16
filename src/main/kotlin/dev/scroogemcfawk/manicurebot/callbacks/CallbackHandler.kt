package dev.scroogemcfawk.manicurebot.callbacks

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.scroogemcfawk.manicurebot.config.Config
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.Appointment
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

private val log = LoggerFactory.getLogger("CallbackHandler.kt")


@Suppress("SpellCheckingInspection")
class CallbackHandler(
    private val ctx: BehaviourContext, private val config: Config, private val locale: Locale,
    private val
    appointments: ArrayList<Appointment>,
) {

    private val bot = ctx.bot
    private val scope = ctx.scope
    private val manager = config.manager

    private var add: LocalDate? = null

    private val dateFormat = DateTimeFormatter.ofPattern(locale.dateFormat)
    private val dateTimeFormat = DateTimeFormatter.ofPattern(locale.dateTimeFormat)

    private val log = LoggerFactory.getLogger(this::class.java)!!

    private suspend fun answerEmpty(cb: DataCallbackQuery) {
        bot.answerCallbackQuery(cb, "")
    }

    private suspend fun answerInvalid(cb: DataCallbackQuery) {
        bot.answerCallbackQuery(cb, "Invalid callback.")
    }

    private suspend fun notifyUser(id: Long, appointment: Appointment) {
        val dif = ChronoUnit.MILLIS.between(LocalDateTime.now(), appointment.datetime)
        if (dif >= 80_000) {
            log.info("Suspended call on ${appointment.datetime} in ${dif - 60000}")
            delay(dif - 60_000)
            bot.send(
                ChatId(id), "Вы записаны на ${appointment.datetime.format(dateTimeFormat)},\n" +
                        "по адресу Ленина 151/1 и дт и тп"
            )
        }
    }

    suspend fun processCallback(cb: DataCallbackQuery) {
        log.info(cb.data)
        val (source, data) = cb.data.split(":", limit = 2)
        when (source) {
            "empty" -> {
                answerEmpty(cb)
            }

            "add" -> {
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
                            cb.message!!.chat.id, cb.message!!.messageId, "Select time:"
                        )
                        this.bot.editMessageReplyMarkup(
                            cb.message!!.chat.id,
                            cb.message!!.messageId,
                            replyMarkup = getInlineClockMarkup()
                        )
                    }

                    "time" -> {
                        val (hs, ms) = value.split(":")
                        val h = hs.split("=")[1].toInt()
                        val m = ms.split("=")[1].toInt()
                        this.bot.answerCallbackQuery(cb, "")
                        this.bot.editMessageText(
                            cb.message!!.chat.id,
                            cb.message!!.messageId,
                            "Запись добавлена ${h}:${m.toString().padStart(2, '0')} ${
                                add!!.dayOfMonth
                            }.${add!!.month}.${add!!.year}"
                        )
                        appointments.add(
                            Appointment(
                                LocalDateTime.of(
                                    add!!.year, add!!.month, add!!.dayOfMonth, h, m
                                ), cb.user.id.chatId
                            )
                        )
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

            "calendar" -> {
                val (action, value) = data.split(":", limit = 2)
                val (yearArg, monthArg) = value.split(":", limit = 2)

                val year = Year.of(yearArg.split("=")[1].toInt())
                val month = Month.valueOf(monthArg.split("=")[1])
                when (action) {
                    "prevMonth" -> {
                        val prev = YearMonth.of(year.value, month).minusMonths(1)
                        this.bot.answerCallbackQuery(cb, "Switching to $prev")
                        this.bot.editMessageReplyMarkup(
                            cb.message!!.chat.id,
                            cb.message!!.messageId,
                            replyMarkup = getInlineCalendarMarkup(prev, locale)
                        )
                    }

                    "nextMonth" -> {
                        val next = YearMonth.of(year.value, month).plusMonths(1)
                        this.bot.answerCallbackQuery(cb, "Switching to $next")
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

            "signup" -> {
                try {
                    val (idPair, app) = data.split(":", limit = 2)

                    val id = restore<Long>(idPair)!!
                    val appointment = restore<Appointment>(app)!!

                    if (appointments.isStillAvailable(appointment)) {
                        appointments.occupy(appointment, id)
                        scope.launch {
                            notifyUser(id, appointment)
                        }
                        answerEmpty(cb)
                        bot.editMessageText(
                            cb.message!!.chat,
                            cb.message!!.messageId,
                            "Вы записаны на ${appointment.datetime.format(dateTimeFormat)}",
                            replyMarkup =
                            null
                        )
                        bot.send(
                            manager,
                            "На ${appointment.datetime.format(dateFormat)} Записан чубрик " + cb.user.username!!.username
                        )
                    } else {
                        bot.answerCallbackQuery(cb.id, "Huinee")
                    }
                } catch (e: Exception) {
                    println(e.message)
                }
            }

            else -> {
                answerInvalid(cb)
                log.warn("Invalid source: $source")
            }
        }
    }
}

private fun HashMap<String, String>.fromCallbackArgs(args: List<String>): HashMap<String,
        String> {
    for (p in args) {
        val (k, v) = p.split("=")
        this[k] = v
    }
    return this
}

private inline fun <reified T: Any?> restore(s: String): T? {
    when (T::class) {
        Long::class -> {
            return s.split("=")[1].toLong() as T
        }

        Appointment::class -> {
            val args = s.split(":")
            val map = HashMap<String, String>().fromCallbackArgs(args)
            val ld =
                LocalDate.of(map["Y"]!!.toInt(), Month.valueOf(map["M"]!!), map["D"]!!.toInt())
            val lt = LocalTime.of(map["h"]!!.toInt(), map["m"]!!.toInt())
            val id = map["id"]!!
            return Appointment(
                LocalDateTime.of(ld, lt),
                if (id == "null") null else id.toLong()
            )
                    as T
        }

        else -> {
            log.error("Unexpected type found.")
            return null
        }
    }
}
