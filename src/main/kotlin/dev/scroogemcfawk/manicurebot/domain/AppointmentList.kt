package dev.scroogemcfawk.manicurebot.domain

import dev.scroogemcfawk.manicurebot.isFuture
import korlibs.time.DateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppointmentList(
    dateTimePattern: String
) {
    private val dateTimeFormat = DateTimeFormatter.ofPattern(dateTimePattern)
    private val appointments = ArrayList<Appointment>()

    val all: List<Appointment>
        get() = this.appointments

    val allFuture: List<Appointment>
        get() = this.appointments.filter { it.datetime > LocalDateTime.now() }

    fun add(a: Appointment) {
        appointments.add(a)
    }

    fun cancel(a: Appointment) {
        for (e in appointments) {
            if (e == a) {
                e.client = null
                return
            }
        }
    }

    fun delete(a: Appointment) {
        appointments.remove(a)
    }

    fun getFutureAppointmentOrNull(chatId: Long): Appointment? {
        val now = LocalDateTime.now()
        for (e in appointments) {
            if (e.client == chatId && e.datetime > now) return e
        }
        return null
    }

    fun getClientAppointmentOrNull(chatId: Long): Appointment? {
        for (e in appointments) {
            if (e.client == chatId && e.datetime.isFuture()) return e
        }
        return null
    }

    fun hasAvailable(): Boolean {
        for (e in appointments) {
            if (e.client == null) {
                return true
            }
        }
        return false
    }

    fun assignClient(a: Appointment, id: Long?) {
        for (e in appointments) {
            if (e == a) {
                e.client = id
            }
        }
    }

    fun isAvailable(a: Appointment): Boolean {
        // good enough because appointments are not endless
        for (e in appointments) {
            if (e == a) {
                return e.client == null
            }
        }
        return false
    }

    fun clientHasAppointment(chatId: Long): Boolean {
        for (e in appointments) {
            if (e.client == chatId && e.datetime.isFuture()) return true
        }
        return false
    }

    fun joinToString(
        s: String,
        transform: ((Appointment) -> String) = {
            it.datetime.format(dateTimeFormat)
        }
    ): String {
        return this.appointments.joinToString(s) { transform(it) }
    }

    fun reschedule(old: Appointment, new: Appointment): Boolean {
        if (isAvailable(new)) {
            assignClient(new, old.client)
            assignClient(old, null)
            return true
        }
        return false
    }
}