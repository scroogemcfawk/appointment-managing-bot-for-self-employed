package smf.samurai1.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import smf.samurai1.config.Locale
import smf.samurai1.repository.AppointmentRepo


fun getRescheduleMarkupInline(
    chatId: Long,
    appointments: AppointmentRepo,
    locale: Locale,
//    session: Stack<String>
): InlineKeyboardMarkup {
    return getInlineAvailableAppointmentsMarkup(chatId, appointments, locale, "L")
}
