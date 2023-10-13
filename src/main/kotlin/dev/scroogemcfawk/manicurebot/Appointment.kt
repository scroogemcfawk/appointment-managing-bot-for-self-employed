package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

data class Appointment(val datetime: LocalDateTime, val client: ChatId?) : CallbackStringable {
    val date = datetime.toLocalDate()
    val time = datetime.toLocalTime()

    override fun toCallbackString(): String {
        return "Y=${date.year}:M=${date.month}:D=${date.dayOfMonth}:h=${
            time.hour
        }:m=${time.minute}:id=$client"
    }
}

fun <Appointment> restore(s: String): dev.scroogemcfawk.manicurebot.Appointment {
    val args = s.split(":")
    val map = HashMap<String, String>().fromCallbackArgs(args)
    val ld = LocalDate.of(map["Y"]!!.toInt(), Month.valueOf(map["M"]!!), map["D"]!!.toInt())
    val lt = LocalTime.of(map["h"]!!.toInt(), map["m"]!!.toInt())
    return Appointment(LocalDateTime.of(ld, lt), ChatId(map["id"]!!.toLong()))
}

