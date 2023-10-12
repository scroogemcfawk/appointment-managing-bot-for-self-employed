package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.types.ChatId
import korlibs.time.DateTime

data class Appointment(val datetime: DateTime, val client: ChatId?) {
    val date = datetime.date
    val time = datetime.time
}