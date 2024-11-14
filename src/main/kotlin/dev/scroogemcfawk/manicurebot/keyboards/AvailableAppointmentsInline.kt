package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.RowBuilder
import dev.inmo.tgbotapi.utils.plus
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.AppointmentRepo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun getInlineAvailableAppointmentsMarkup(
    chatId: Long,
    appointments: AppointmentRepo,
    locale: Locale,
    callBackSource: String = locale.appointmentCommand,
    excludeUserChat: Long? = null
): InlineKeyboardMarkup {
    val mb = MatrixBuilder<InlineKeyboardButton>()
    mb.addAvailableAppointments(chatId, appointments, locale, callBackSource, excludeUserChat)
    return InlineKeyboardMarkup(mb.matrix)
}

private fun MatrixBuilder<InlineKeyboardButton>.addAvailableAppointments(
    chatId: Long,
    appointments: AppointmentRepo,
    locale: Locale,
    callBackSource: String = locale.appointmentCommand,
    excludeUserChat: Long? = null
) {
    val dateTimeFormatter = DateTimeFormatter.ofPattern(locale.dateTimeFormat)
    val availableAppointments = appointments.all
        .asSequence()
        .filter { ChronoUnit.MINUTES.between(LocalDateTime.now(), it.datetime) > 1 }
        .filter { it.client == null }
        .filter { appointment ->
            if (excludeUserChat != null) excludeUserChat != appointment.client else true
        }
        .filter { it.datetime > LocalDateTime.now() }
        .sortedWith(compareBy({ it.date }, { it.time }))
        .toList()
    for (a in availableAppointments) {
        val rb = RowBuilder<InlineKeyboardButton>()
        val label = a.datetime.format(dateTimeFormatter)
        val data = "${callBackSource}:id=${chatId}:${a.toCallbackString()}"
        if (data.length > 64) println(data)
        val btn = dataInlineButton(label, data)

        rb + btn

        this.add(rb.row)
    }
    this.add(listOf(dataInlineButton(locale.cancelOperation, "${callBackSource}:cancel")))
}
