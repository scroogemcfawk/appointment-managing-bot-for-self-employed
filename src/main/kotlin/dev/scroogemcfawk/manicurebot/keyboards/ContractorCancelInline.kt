package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.RowBuilder
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.AppointmentList
import java.time.format.DateTimeFormatter

fun getContractorCancelInline(
    contractor: Long,
    appointments: AppointmentList,
    dateTimeFormat: DateTimeFormatter,
    locale: Locale
): InlineKeyboardMarkup {
    val mb = MatrixBuilder<InlineKeyboardButton>()

    for (a in appointments.all.filter { it.client == contractor }.sortedBy { it.datetime }) {
        val rb = RowBuilder<InlineKeyboardButton>()
        rb.add(
            dataInlineButton(
                a.datetime.format(dateTimeFormat),
                "c:${locale.cancelCommand}:${a.toCallbackString()}"
            )
        )
        mb.add(rb.row)
    }

    return InlineKeyboardMarkup(mb.matrix)
}