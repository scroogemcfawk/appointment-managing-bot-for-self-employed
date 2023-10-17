package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.RowBuilder
import dev.inmo.tgbotapi.utils.plus
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.AppointmentList
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*


fun getRescheduleMarkupInline(
    chatId: Long,
    appointments: AppointmentList,
    locale: Locale,
//    session: Stack<String>
): InlineKeyboardMarkup {
    return getInlineAvailableAppointmentsMarkup(chatId, appointments, locale, "L")
}
