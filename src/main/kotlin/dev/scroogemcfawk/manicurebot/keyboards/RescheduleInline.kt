package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.AppointmentRepo


fun getRescheduleMarkupInline(
    chatId: Long,
    appointments: AppointmentRepo,
    locale: Locale,
//    session: Stack<String>
): InlineKeyboardMarkup {
    return getInlineAvailableAppointmentsMarkup(chatId, appointments, locale, "L")
}
