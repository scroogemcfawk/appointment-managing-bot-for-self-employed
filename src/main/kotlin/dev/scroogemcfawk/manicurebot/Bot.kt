package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.collections.ArrayList

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

            onCommand("signup", requireOnlyCommandInMessage = true) {
                sendMessage(
                    it.chat.id,
                    "Select day:",
                    replyMarkup = getInlineCalendarMarkup(date = LocalDate.now())
                )
            }

            onDataCallbackQuery { cb ->
                processCallback(cb)
            }

            log.info("Bot is running.")
            log.info(me.toString())
        }
    }

    private suspend fun processCallback(cb: DataCallbackQuery) {
        val (source, data) = cb.data.split(":", limit = 2)
        when (source) {
            "empty" -> {
                answerEmpty(cb)
            }

            "signup" -> {
                val (action, value) = data.split(":", limit = 2)
                when (action) {
                    "select" -> {
                        val (ys, ms, ds) = value.split(":")
                        val y = ys.split("=")[1]
                        val m = ms.split("=")[1]
                        val d = ds.split("=")[1]
                        val ld = LocalDate.of(y.toInt(), m.toInt(), d.toInt())
                        this.bot.answerCallbackQuery(cb, "Запись добавлена на $d.$m.$y")
                        log.info(value)
                        log.info(ld.toString())
                    }

                    else -> {
                        answerInvalid(cb)
                        log.warn("Invalid signup action: $action")
                    }
                }
            }

            else -> {
                answerInvalid(cb)
                log.warn("Invalid source: $source")
            }
        }
    }

    private suspend fun answerEmpty(cb: DataCallbackQuery) {
        this.bot.answerCallbackQuery(cb, "")
    }

    private suspend fun answerInvalid(cb: DataCallbackQuery) {
        this.bot.answerCallbackQuery(cb, "Invalid callback.")
    }


    private fun getInlineCalendarMarkup(date: LocalDate): InlineKeyboardMarkup {
        val mxb = MatrixBuilder<InlineKeyboardButton>()

        for (i in 0..7) {
            val rb = RowBuilder<InlineKeyboardButton>()

            when (i) {
                0 -> {
                    if (date.month > LocalDate.now().month) {
                        rb.add(dataInlineButton("<", "calendar:prevMonth:"))
                    } else {
                        rb.add(dataInlineButton(" ", "empty:"))
                    }
                    rb.add(dataInlineButton("${date.month}", "empty:"))
                    rb.add(dataInlineButton(">", "calendar:nextMonth:"))
                }

                1 -> {
                    rb.add(dataInlineButton(locale.mondayShort, "empty:"))
                    rb.add(dataInlineButton(locale.tuesdayShort, "empty:"))
                    rb.add(dataInlineButton(locale.wednesdayShort, "empty:"))
                    rb.add(dataInlineButton(locale.thursdayShort, "empty:"))
                    rb.add(dataInlineButton(locale.fridayShort, "empty:"))
                    rb.add(dataInlineButton(locale.saturdayShort, "empty:"))
                    rb.add(dataInlineButton(locale.sundayShort, "empty:"))
                }

                else -> {
                    for (b in getRowList(i - 2, date)) {
                        rb.add(b)
                    }
                }
            }
            mxb.add(rb.row)
        }

        return InlineKeyboardMarkup(
            mxb.matrix
        )
    }

    private fun getRowList(r: Int, date: LocalDate): java.util.ArrayList<InlineKeyboardButton> {
        val ret = ArrayList<InlineKeyboardButton>()
        val year = date.year
        val month = date.month.ordinal
        val firstDayOfWeekInMonth = LocalDate.of(date.year, date.month, 1).dayOfWeek.ordinal
        val daysInFirstWeekInMonth = 7 - firstDayOfWeekInMonth
        val lastDayOfMonth = LocalDate.of(date.year, date.month.plus(1), 1).minusDays(1).dayOfMonth


        var nonempty = false

        for (i in 0..6) {
            if (r == 0) {
                if (i < firstDayOfWeekInMonth) {
                    ret.add(dataInlineButton(" ", "empty:"))
                } else {
                    val dayNumber = i - firstDayOfWeekInMonth + 1
                    if (dayNumber >= LocalDate.now().dayOfMonth) {
                        val dayLabel =
                            if (dayNumber == LocalDate.now().dayOfMonth) "[ $dayNumber ]" else "$dayNumber"
                        ret.add(
                            dataInlineButton(
                                dayLabel,
                                "signup:select:year=$year:month=${month + 1}:day=$dayNumber"
                            )
                        )
                        nonempty = true
                    } else {
                        ret.add(
                            dataInlineButton(
                                " ",
                                "empty:"
                            )
                        )
                    }
                }
            } else {
                val dayNumber = i - firstDayOfWeekInMonth + 1 + r * 7
                if (dayNumber <= lastDayOfMonth) {
                    val dayLabel =
                        if (dayNumber == LocalDate.now().dayOfMonth) "[ $dayNumber ]" else "$dayNumber"
                    ret.add(
                        dataInlineButton(
                            dayLabel,
                            "signup:select:year=$year:month=${month + 1}:day=$dayNumber"
                        )
                    )
                    nonempty = true
                } else {
                    ret.add(dataInlineButton(" ", "empty:"))
                }
            }
        }
        if (!nonempty) {
            return ArrayList()
        }
        return ret
    }
}