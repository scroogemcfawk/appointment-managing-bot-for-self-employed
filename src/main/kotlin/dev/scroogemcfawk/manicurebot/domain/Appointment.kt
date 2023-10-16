package dev.scroogemcfawk.manicurebot.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Appointment(val datetime: LocalDateTime, var client: Long? = null) {
    val date: LocalDate = datetime.toLocalDate()
    val time: LocalTime = datetime.toLocalTime()

    fun toCallbackString(): String {
        return "Y=${date.year}:M=${date.month}:D=${date.dayOfMonth}:h=${
            time.hour
        }:m=${time.minute}:id=$client"
    }
}

fun ArrayList<Appointment>.getFutureAppointmentOrNull(chatId: Long): Appointment? {
    val now = LocalDateTime.now()
    for (e in this) {
        if (e.client == chatId && e.datetime > now) return e
    }
    return null
}

fun ArrayList<Appointment>.hasAvailable(): Boolean {
    for (e in this) {
        if (e.client == null) {
            return true
        }
    }
    return false
}

fun ArrayList<Appointment>.occupy(app: Appointment, id: Long) {
    for (e in this) {
        if (e.datetime == app.datetime) {
            e.client = id
        }
    }
}

fun ArrayList<Appointment>.isStillAvailable(target: Appointment): Boolean {
    // good enough because appointments are not endless
    for (a in this) {
        if (a.datetime == target.datetime) {
            return a.client == null
        }
    }
    return false
}