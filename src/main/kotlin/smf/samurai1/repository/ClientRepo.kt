package smf.samurai1.repository

import org.tinylog.Logger
import smf.samurai1.entity.Client
import java.sql.Connection
import java.sql.ResultSet


class ClientRepo(private val con: Connection) {

    init {
        val statement = con.createStatement()
        statement.execute(SQL.CLIENT.CREATE)
    }

    operator fun get(id: Long): Client? {
        try {
            val statement = con.prepareStatement(SQL.CLIENT.SELECT_BY_ID) {
                setLong(1, id)
            }

            val rs = statement.executeQuery()

            if (rs.next()) {
                return rs.toClient()
            }

            return null
        } catch (e: Exception) {
            Logger.error { "Get: $e" }
            throw e
        }
    }

    operator fun set(id: Long, c: Client) {
        try {
            if (contains(id)) return

            val number = c.phoneNumber.ifBlank { "no number" }

            val statement = con.prepareStatement(SQL.CLIENT.INSERT) {
                setLong(1, id)
                setString(2, c.name)
                setString(3, number)
            }

            statement.execute()
        } catch (e: Exception) {
            Logger.error { "Set: $e" }
            throw e
        }
    }

    operator fun contains(id: Long): Boolean {
        val statement = con.prepareStatement(SQL.CLIENT.COUNT) {
            setLong(1, id)
        }

        val rs = statement.executeQuery()

        if (rs.next()) {
            return rs.getInt(1) > 0
        }

        return false
    }

    fun toList(): List<Pair<Long, Client>> {

        val statement = con.prepareStatement(SQL.CLIENT.SELECT_ALL)
        val rs = statement.executeQuery()

        val local = HashMap<Long, Client>()

        while (rs.next()) {
            val client = rs.toClient()
            local[client.id] = client
        }

        return local.toList()
    }

    companion object {

        fun ResultSet.toClient(): Client {
            return Client(
                getLong(1),
                getString(2),
                getString(3)
            )
        }
    }
}
