package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.RowBuilder
import dev.scroogemcfawk.manicurebot.domain.AppointmentList
import java.time.format.DateTimeFormatter

fun getAppointmentListInlineMarkup(
    chatId: Long,
    appointments: AppointmentList,
    dateTimeFormat: DateTimeFormatter,
    callbackBase: String
): InlineKeyboardMarkup {

    val mb = MatrixBuilder<InlineKeyboardButton>()

    for (a in appointments.allFuture.filter { it.client == chatId }) {
        val rb = RowBuilder<InlineKeyboardButton>()
        rb.add(
            dataInlineButton(
                a.datetime.format(dateTimeFormat),
                "${callbackBase}:${a.toCallbackString()}"
            )
        )
        mb.add(rb.row)
    }

    return InlineKeyboardMarkup(mb.matrix)
}