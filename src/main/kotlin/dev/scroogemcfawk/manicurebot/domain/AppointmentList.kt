package dev.scroogemcfawk.manicurebot.domain

import dev.scroogemcfawk.manicurebot.isFuture
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppointmentList(
    dateTimePattern: String,
    private val con: Connection
) {
    private val log = LoggerFactory.getLogger(Appointment::class.java)
    private val dateTimeFormat = DateTimeFormatter.ofPattern(dateTimePattern)
    private val appointments = ArrayList<Appointment>()

    @Suppress("DuplicatedCode")
    val all: List<Appointment>
        get() {
            val sql = "select * from appointment"
            val s = con.createStatement()
            val rs = s.executeQuery(sql)

            val local = ArrayList<Appointment>()

            while (rs.next()) {
                val ts = rs.getTimestamp(2)
                val client = if (rs.getLong(3) == 0L) null else rs.getLong(3)
                val app = Appointment(ofTimeStamp(ts.toString()), client)
                local.add(app)
            }

            return local
        }

    @Suppress("DuplicatedCode")
    val allFuture: List<Appointment>
        get() {
            val sql = "select * from appointment where datetime > systimestamp"
            val s = con.createStatement()
            val rs = s.executeQuery(sql)

            val local = ArrayList<Appointment>()

            while (rs.next()) {
                val ts = rs.getTimestamp(2)
                val client = if (rs.getLong(3) == 0L) null else rs.getLong(3)
                val app = Appointment(ofTimeStamp(ts.toString()), client)
                local.add(app)
            }

            return local

//            return appointments.filter { it.datetime > LocalDateTime.now() }
        }


    fun add(a: Appointment) {
        appointments.add(a)
        addToTable(a)
        clearOld()
    }

    private fun clearOld() {
        val sql = "delete from APPOINTMENT where DATETIME < systimestamp"
        val s = con.createStatement()
        s.execute(sql)
    }

    private fun addToTable(a: Appointment) {
        try {
            val sql = "insert into appointment(datetime, client) values('${a.datetime}', ${a.client});"
            val s = con.createStatement()
            s.execute(sql)
        } catch (e: Exception) {
            log.error(e.message)
        }
    }

    fun cancel(a: Appointment) {
        for (e in appointments) {
            if (e == a) {
                e.client = null
                break
            }
        }
        cancelInTable(a)
    }

    private fun cancelInTable(a: Appointment) {
        log.debug(a.toString())
        val sql = "update appointment set client = null where DATETIME = '${a.datetime}' and client = ${a.client}"
        val s = con.createStatement()
        s.execute(sql)
    }

    fun delete(a: Appointment) {
        appointments.remove(a)
        deleteInTable(a)
    }

    private fun deleteInTable(a: Appointment) {
        val sql = "delete from appointment where DATETIME = '${a.datetime}'"
        val s = con.createStatement()
        s.execute(sql)
    }

    fun getFutureAppointmentOrNull(chatId: Long): Appointment? {
        for (e in allFuture) {
            if (e.client == chatId) return e
        }
        return null
    }

    // TODO: remove this shit
    fun getClientAppointmentOrNull(chatId: Long): Appointment? = getFutureAppointmentOrNull(chatId)

    fun hasAvailable(): Boolean {
        for (e in allFuture) {
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
                break
            }
        }
        assignClientInTable(a, id)
    }

    private fun assignClientInTable(a: Appointment, id: Long?) {
        val sql = "update appointment set client = $id where DATETIME = '${a.datetime}' "
        val s = con.createStatement()
        s.execute(sql)
    }

    fun isAvailable(a: Appointment): Boolean {
        // good enough because appointments are not endless
        for (e in all) {
            if (e == a) {
                return e.client == null
            }
        }
        return false
    }

    fun clientHasAppointment(chatId: Long): Boolean {
        for (e in allFuture) {
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
        return this.all.joinToString(s) { transform(it) }
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

private fun ofTimeStamp(ts: String): LocalDateTime {
    val (year, month, day) = ts.split(" ")[0].split("-").map { it.toInt() }
    val (hour, minute) = ts.split(" ")[1].split(":").slice(0..1).map { it.toInt() }
    return LocalDateTime.of(year, month, day, hour, minute)
}
