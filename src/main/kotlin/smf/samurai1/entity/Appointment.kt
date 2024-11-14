package smf.samurai1.entity

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
