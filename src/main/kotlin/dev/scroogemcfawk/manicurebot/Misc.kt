package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.types.ChatId
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

val log = LoggerFactory.getLogger("Mist.kt")

fun HashMap<String, String>.fromCallbackArgs(args: List<String>): HashMap<String, String> {
    for (p in args) {
        val (k, v) = p.split("=")
        this[k] = v
    }
    return this
}


inline fun <reified T: Any?> restore(s: String): T? {
    when (T::class) {
        Long::class -> {
            return s.split("=")[1].toLong() as T
        }

        Appointment::class -> {
            val args = s.split(":")
            val map = HashMap<String, String>().fromCallbackArgs(args)
            val ld = LocalDate.of(map["Y"]!!.toInt(), Month.valueOf(map["M"]!!), map["D"]!!.toInt())
            val lt = LocalTime.of(map["h"]!!.toInt(), map["m"]!!.toInt())
            val id = map["id"]!!
            return Appointment(LocalDateTime.of(ld, lt), if (id == "null") null else id.toLong())
                    as T
        }

        else -> {
            log.error("Unexpected type found.")
            return null
        }
    }
}