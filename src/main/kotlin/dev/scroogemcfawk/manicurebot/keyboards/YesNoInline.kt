package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.flatMatrix
import dev.scroogemcfawk.manicurebot.domain.Appointment

fun getYesNoInlineMarkup(a: Appointment): InlineKeyboardMarkup {
    val mb = flatMatrix(
        dataInlineButton("Yes", "cancel:yes:${a.toCallbackString()}"),
        dataInlineButton("No", "cancel:no")
    )
    return InlineKeyboardMarkup(mb)
}