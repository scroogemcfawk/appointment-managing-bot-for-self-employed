package smf.samurai1.repository

import org.tinylog.Logger
import smf.samurai1.entity.Appointment
import smf.samurai1.isFuture
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppointmentRepo(
    dateTimePattern: String,
    private val con: Connection
) {

    private val dateTimeFormat = DateTimeFormatter.ofPattern(dateTimePattern)
    private val appointments = ArrayList<Appointment>()

    init {
        val statement = con.createStatement()
        statement.execute(SQL.APPOINTMENT.CREATE)
    }

    @Suppress("DuplicatedCode")
    val all: List<Appointment>
        get() {
            val statement = con.prepareStatement(SQL.APPOINTMENT.SELECT_ALL)

            val rs = statement.executeQuery()

            val local = ArrayList<Appointment>()

            while (rs.next()) {
                local.add(rs.toAppointment())
            }

            return local
        }

    @Suppress("DuplicatedCode")
    val allFuture: List<Appointment>
        get() {
            val statement = con.prepareStatement(SQL.APPOINTMENT.SELECT_ALL_IN_FUTURE)

            val rs = statement.executeQuery()

            val local = ArrayList<Appointment>()

            while (rs.next()) {
                local.add(rs.toAppointment())
            }

            return local
        }

    fun add(a: Appointment) {
        appointments.add(a)
        addToTable(a)
    }

    fun clearOld() {
        val statement = con.prepareStatement(SQL.APPOINTMENT.DELETE_ALL_IN_PAST)
        statement.execute()
    }

    private fun addToTable(a: Appointment) {
        try {
            val s = con.prepareStatement(SQL.APPOINTMENT.INSERT) {
                setString(1, a.datetime.toString())
                a.client ?.let{
                    setLong(2, it)
                } ?: setNull(2, Types.INTEGER)
            }
            s.execute()
        } catch (e: Exception) {
            Logger.error { e }
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
        Logger.debug { a.toString() }
        if (a.client == null) {
            return
        }
        val statement = con.prepareStatement(SQL.APPOINTMENT.CANCEL) {
            setString(1, a.datetime.toString())
            setLong(2, a.client!!)
        }
        statement.execute()
    }

    fun delete(a: Appointment) {
        appointments.remove(a)
        deleteInTable(a)
    }

    private fun deleteInTable(a: Appointment) {
        val s = con.prepareStatement(SQL.APPOINTMENT.DELETE) {
            setString(1, a.datetime.toString())
        }
        s.execute()
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
        val statement = con.prepareStatement(SQL.APPOINTMENT.ASSIGN_CLIENT) {
            id ?.let{
                setLong(1, it)
            } ?: setNull(1, Types.INTEGER)
            setString(2, a.datetime.toString())
        }
        statement.execute()
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

    companion object {
        private fun ofTimeStamp(ts: String): LocalDateTime {
            val split = ts.split("T")
            val (year, month, day) = split[0].split("-").map { it.toInt() }
            val (hour, minute) = split[1].split(":").slice(0..1).map { it.toInt() }
            return LocalDateTime.of(year, month, day, hour, minute)
        }

        private fun ResultSet.toAppointment(): Appointment {
            val datetime = getString(2)
            val client = if (getLong(3) == 0L) null else getLong(3)
            return Appointment(ofTimeStamp(datetime), client)
        }
    }
}
