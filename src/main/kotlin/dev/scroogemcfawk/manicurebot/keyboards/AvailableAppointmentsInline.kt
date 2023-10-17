package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.RowBuilder
import dev.inmo.tgbotapi.utils.plus
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.Appointment
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun getInlineAvailableAppointmentsMarkup(
    chatId: Long,
    appointments: ArrayList<Appointment>,
    locale: Locale
): InlineKeyboardMarkup {
    val mb = MatrixBuilder<InlineKeyboardButton>()
    mb.addAvailableAppointments(chatId, appointments, locale)
    return InlineKeyboardMarkup(mb.matrix)
}

private fun MatrixBuilder<InlineKeyboardButton>.addAvailableAppointments(
    chatId: Long,
    appointments: ArrayList<Appointment>,
    locale: Locale
) {
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
        val data = "${locale.appointmentCommand}:id=${chatId}:${a.toCallbackString()}"
        if (data.length > 64) println(data)
        val btn = dataInlineButton(label, data)

        rb + btn

        this.add(rb.row)
    }
}