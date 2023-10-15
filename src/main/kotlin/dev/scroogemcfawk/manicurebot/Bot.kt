package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.asPrivateChat
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.RowBuilder
import dev.inmo.tgbotapi.utils.plus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.slf4j.LoggerFactory
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.reflect.jvm.internal.impl.load.java.JavaClassFinder.Request

class Bot(token: String, val locale: Locale, val dev: ChatId, val owner: ChatId) {

    val bot = telegramBot(token)
    val scope = CoroutineScope(Dispatchers.Default)
    val log = LoggerFactory.getLogger(Bot::class.java)

    val managers = ArrayList<ChatId>()

    var add: LocalDate? = null

    val target = dev

    val appointments = ArrayList<Appointment>()
    val users = HashMap<Long, User>()

    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yy")
    val dateTimeFormat = DateTimeFormatter.ofPattern("HH:mm dd.MM.yy")

    init {
        managers.add(dev)
        managers.add(owner)
        mock()
    }

    fun mock() {
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 10, 11, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 15, 15, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 16, 12, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 23, 17, 45), null))

        appointments.add(Appointment(getLDTAfterMinutes(1), null))
        appointments.add(Appointment(getLDTAfterMinutes(2), null))
        appointments.add(Appointment(getLDTAfterMinutes(3), null))
        appointments.add(Appointment(getLDTAfterMinutes(4), null))
//        appointments.add(Appointment(getLDTAfterMinutes(9), null))
    }

    private fun getLDTAfterMinutes(m: Int): LocalDateTime {
        val dtin2m = LocalDateTime.now().plusMinutes(m.toLong())
        val ldin2m = dtin2m.toLocalDate()
        val ltin2m = LocalTime.of(dtin2m.hour, dtin2m.minute)
        return LocalDateTime.of(ldin2m, ltin2m)
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

            onCommand("register", requireOnlyCommandInMessage = true) { msg ->
                if (msg.chat.id.chatId !in users) {
                    val name = waitText(
                        SendTextMessage(
                            msg.chat.id,
                            "Your name:"
                        ),
                    ).first().text
                    val phone = waitText(
                        SendTextMessage(
                            msg.chat.id,
                            "Your phone:"
                        ),
                    ).first().text
                    users[msg.chat.id.chatId] = User(msg.chat.id.chatId, name, phone)
                    send(msg.chat.id, "You have been registered.")
                } else {
                    send(msg.chat.id, "You are already registered.")
                }
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

            onCommand("signup", requireOnlyCommandInMessage = true) { msg ->
                if (appointments.hasAvailable()) {
                    appointments.getFutureAppointmentOrNull(msg.chat.id.chatId)
                        ?.let { appointment ->
                            send(
                                msg.chat.id, "You already have an appointment: ${
                                    appointment.datetime.format(
                                        dateTimeFormat
                                    )
                                }"
                            )
                        } ?: run {
                        try {
                            send(
                                msg.chat.id,
                                "Choose an appointment:",
                                replyMarkup = getInlineAvailableAppointmentsMarkup(msg.chat.id.chatId)
                            )
                        } catch (e: Exception) {
                            log.warn("On send: " + e.message)
                        }
                    }
                } else {
                    send(msg.chat.id, "Appointments are not available.")
                }
            }

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

            onCommand("list", requireOnlyCommandInMessage = true) { msg ->
                if (msg.chat.id in managers) {
                    sendMessage(
                        msg.chat.id,
                        appointments.joinToString(",\n") { it.datetime.format(dateTimeFormat) }
                    )
                }
            }

            onCommand("notify", requireOnlyCommandInMessage = true) { msg ->
                if (msg.chat.id in managers) {
                    val text = waitText(SendTextMessage(msg.chat.id, "Notification message:"))
                        .first()
                        .text
                    for ((id, _) in users.toList()) {
                        send(ChatId(id), text)
                    }
                }
            }

            onDataCallbackQuery { cb ->
                processCallback(cb)
            }

            log.info(
                "Bot(id=${me.id.chatId}, ${me.username?.username}, ${me.firstName}) is running."
            )
        }
    }

    private suspend fun processCallback(cb: DataCallbackQuery) {
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
                            replyMarkup = getInlineCalendarMarkup(prev)
                        )
                    }

                    "nextMonth" -> {
                        val next = YearMonth.of(year.value, month).plusMonths(1)
                        this.bot.answerCallbackQuery(cb, "Switching to $next")
                        this.bot.editMessageReplyMarkup(
                            cb.message!!.chat.id,
                            cb.message!!.messageId,
                            replyMarkup = getInlineCalendarMarkup(next)
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

                    if (appointment.isStillAvailable()) {
                        appointments.occupy(appointment, id)
                        scope.launch {
                            notifyUser(id, appointment)
                        }
                        answerEmpty(cb)
                        bot.editMessageText(
                            cb.message!!.chat,
                            cb.message!!.messageId,
                            "Вы " +
                                    "записаны на ${appointment.datetime.format(dateTimeFormat)}",
                            replyMarkup =
                            null
                        )
                        bot.send(
                            target,
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

    private suspend fun notifyUser(id: Long, appointment: Appointment) {
        val dif = ChronoUnit.MILLIS.between(LocalDateTime.now(), appointment.datetime)
        if (dif >= 80_000) {
            println("Suspended call on ${appointment.datetime} in ${dif - 60000}")
            delay(dif - 60_000)
            bot.send(
                ChatId(id), "Вы записаны на ${appointment.datetime.format(dateTimeFormat)},\n" +
                        "по адресу Ленина 151/1 и дт и тп"
            )
        }
    }

    private fun Appointment.isStillAvailable(): Boolean {
        // good enough because appointments are not endless
        for (a in appointments) {
            if (a.datetime == this.datetime) {
                return a.client == null
            }
        }
        return false
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
                            "$day", "add:select:year=${ym.year}:month=${ym.month}:day=$day"
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

    private fun getInlineAvailableAppointmentsMarkup(chatId: Long): InlineKeyboardMarkup {
        val mb = MatrixBuilder<InlineKeyboardButton>()
        try {
            mb.addAvailableAppointments(chatId)
        } catch (e: Exception) {
            log.warn(e.message)
        }
        return InlineKeyboardMarkup(mb.matrix)
    }

    private fun MatrixBuilder<InlineKeyboardButton>.addAvailableAppointments(chatId: Long) {
        val availableAppointments = appointments
            .filter { ChronoUnit.MINUTES.between(LocalDateTime.now(), it.datetime) > 2 }
            .filter { it.client == null }
            .filter { it.datetime > LocalDateTime.now() }
            .sortedWith(compareBy({ it.date }, { it.time }))
        for (a in availableAppointments) {
            val rb = RowBuilder<InlineKeyboardButton>()
            val d = a.date
            val t = a.time
            val label = "$d $t"
            val data = "signup:id=${chatId}:${a.toCallbackString()}"
            val btn = dataInlineButton(label, data)

            rb + btn

            this.add(rb.row)
        }
    }

    fun ArrayList<Appointment>.hasAvailable(): Boolean {
        for (e in this) {
            if (e.client == null) {
                return true
            }
        }
        return false
    }
}

private fun ArrayList<Appointment>.getFutureAppointmentOrNull(chatId: Long): Appointment? {
    val now = LocalDateTime.now()
    for (e in this) {
        if (e.client == chatId && e.datetime > now) return e
    }
    return null
}

private fun ArrayList<Appointment>.occupy(app: Appointment, id: Long) {
    for (e in this) {
        if (e.datetime == app.datetime) {
            e.client = id
        }
    }
}


