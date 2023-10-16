package dev.scroogemcfawk.manicurebot.keyboards

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.RowBuilder
import dev.scroogemcfawk.manicurebot.config.Locale
import java.time.LocalDate
import java.time.YearMonth

fun getInlineCalendarMarkup(ym: YearMonth, locale: Locale): InlineKeyboardMarkup {
    val mb = MatrixBuilder<InlineKeyboardButton>()

    mb.addMonthRow(ym)
    mb.addDayOfWeekRow(locale)
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

private fun MatrixBuilder<InlineKeyboardButton>.addDayOfWeekRow(locale: Locale) {
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