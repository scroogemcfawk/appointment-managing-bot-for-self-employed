package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.RowBuilder

fun getInlineClockMarkup(): InlineKeyboardMarkup {
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