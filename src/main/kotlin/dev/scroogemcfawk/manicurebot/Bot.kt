package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.*
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.time.YearMonth
import kotlin.collections.ArrayList

class Bot(token: String, val locale: Locale, val dev: ChatId, val owner: ChatId) {

    val bot = telegramBot(token)
    val scope = CoroutineScope(Dispatchers.Default)
    val log = LoggerFactory.getLogger(Bot::class.java)

    val managers = ArrayList<ChatId>()

    var add: LocalDate? = null

    val appointments = ArrayList<LocalDateTime>()

    init{
        managers.add(dev)
        managers.add(owner)
    }

    suspend fun run(): Job {
        return bot.buildBehaviourWithLongPolling(scope) {
            val me = getMe()

            //===============COMMON COMMANDS==============================

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

            //===============CLIENT COMMANDS==============================



            //===============MANAGER COMMANDS==============================

            onCommand("add", requireOnlyCommandInMessage = true) {
                if (it.chat.id in managers) {
                    sendMessage(
                        it.chat.id,
                        "Select day:",
                        replyMarkup = getInlineCalendarMarkup(YearMonth.now())
                    )
                }
            }

            onCommand("list", requireOnlyCommandInMessage = true) {
                if (it.chat.id in managers) {
                    val strs = ArrayList<String>()
                    val str = appointments.forEach{ strs.add("${it.hour}:${it.minute.toString().padStart(2, '0')
                    } ${it.dayOfMonth}.${it.month.value}.${it.year}") }
                    sendMessage(
                        it.chat.id,
                        strs.joinToString(", ")
                    )
                }
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
                            cb.message!!.chat.id, cb.message!!
                                .messageId, "Select time:"
                        )
                        this.bot.editMessageReplyMarkup(
                            cb.message!!.chat.id, cb.message!!
                                .messageId, replyMarkup = getInlineClockMarkup()
                        )
                    }

                    "time" -> {
                        val (hs, ms) = value.split(":")
                        val h = hs.split("=")[1].toInt()
                        val m = ms.split("=")[1].toInt()
                        this.bot.answerCallbackQuery(cb, "")
                        this.bot.editMessageText(
                            cb.message!!.chat.id, cb.message!!
                                .messageId,
                            "Запись добавлена ${h}:${m.toString().padStart(2, '0')} ${add
                                !!.dayOfMonth}.${add!!.month}.${add!!.year}"
                        )
                        appointments.add(LocalDateTime.of(
                            add!!.year, add!!.month, add!!.dayOfMonth, h,
                            m))
                        this.bot.editMessageReplyMarkup(
                            cb.message!!.chat.id, cb.message!!
                                .messageId, replyMarkup = null
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
                            cb.message!!.chat.id, cb.message!!
                                .messageId, replyMarkup = getInlineCalendarMarkup(prev)
                        )
                    }

                    "nextMonth" -> {
                        val next = YearMonth.of(year.value, month).plusMonths(1)
                        this.bot.answerCallbackQuery(cb, "Switching to $next")
                        this.bot.editMessageReplyMarkup(
                            cb.message!!.chat.id, cb.message!!
                                .messageId, replyMarkup = getInlineCalendarMarkup(next)
                        )
                    }

                    else -> {
                        answerInvalid(cb)
                        log.warn("Invalid calendar action: $action")
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

    private fun getInlineClockMarkup(): InlineKeyboardMarkup {
        val mb = MatrixBuilder<InlineKeyboardButton>()
        for (h in 9..20) {
            mb.addHoursRow(h)
        }

        return InlineKeyboardMarkup(mb.matrix)
    }

    private fun MatrixBuilder<InlineKeyboardButton>.addHoursRow(h: Int) {
        val rb = RowBuilder<InlineKeyboardButton>()
        for (m in 0..45 step 15) {
            rb.add(dataInlineButton("$h:${m.toString().padStart(2, '0')}", "add:time:h=$h:m=$m"))
        }
        this.add(rb.row)
    }


    private fun getInlineCalendarMarkup(ym: YearMonth): InlineKeyboardMarkup {
        val mb = MatrixBuilder<InlineKeyboardButton>()

        mb.addMonthRow(ym)
        mb.addDayOfWeekRow()
        mb.addMonthLayout(ym)

        return InlineKeyboardMarkup(mb.matrix)
    }

    private fun MatrixBuilder<InlineKeyboardButton>.addMonthRow(ym: YearMonth) {
        val row = RowBuilder<InlineKeyboardButton>()
        if (ym > YearMonth.now()) {
            row.add(
                dataInlineButton("<", "calendar:prevMonth:year=${ym.year}:month=${ym.month}")
            )
        } else {
            row.add(dataInlineButton(" ", "empty:"))
        }
        row.add(dataInlineButton("${ym.month}", "empty:"))
        row.add(dataInlineButton(">", "calendar:nextMonth:year=${ym.year}:month=${ym.month}"))

        this.add(row.row)
    }

    private fun MatrixBuilder<InlineKeyboardButton>.addDayOfWeekRow() {
        this.add(
            listOf(
                dataInlineButton(locale.mondayShort, "empty:"),
                dataInlineButton(locale.tuesdayShort, "empty:"),
                dataInlineButton(locale.wednesdayShort, "empty:"),
                dataInlineButton(locale.thursdayShort, "empty:"),
                dataInlineButton(locale.fridayShort, "empty:"),
                dataInlineButton(locale.saturdayShort, "empty:"),
                dataInlineButton(locale.sundayShort, "empty:")
            )
        )
    }

    private fun MatrixBuilder<InlineKeyboardButton>.addMonthLayout(ym: YearMonth) {
        val firstDMY = LocalDate.of(ym.year, ym.month, 1)
        val firstDOW = firstDMY.dayOfWeek
        val lastDMY = firstDMY.plusMonths(1).minusDays(1).dayOfMonth

        var rowCount = 0
        var stop = false
        while (!stop) {
            val rb = RowBuilder<InlineKeyboardButton>()
            for (i in 0..6) {
                val day = rowCount * 7 + i + 1 - firstDOW.ordinal
                if (day == lastDMY) stop = true
                if (day in 1..lastDMY) {
                    rb.add(
                        dataInlineButton(
                            "$day",
                            "add:select:year=${ym.year}:month=${ym.month}:day=$day"
                        )
                    )
                } else {
                    rb.add(dataInlineButton(" ", "empty:"))
                }
            }
            this.add(rb.row)
            ++rowCount
        }
    }
}


