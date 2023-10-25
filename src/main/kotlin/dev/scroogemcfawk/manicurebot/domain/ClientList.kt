package dev.scroogemcfawk.manicurebot.domain

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection

class ClientList(private val con: Connection) {
//    private val clientChats = HashMap<Long, Client>()
private val log: Logger = LoggerFactory.getLogger("ClientList")

    operator fun get(id: Long): Client? {

        try {
            val sql = "select * from client where id = '$id'"
            val statement = con.createStatement()
            val rs = statement.executeQuery(sql)

            return rs?.run {
                rs.next()
                Client(
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getString(3)
                )
            }
        } catch (e: Exception) {
            log.error("Get: ${e.message}")
            throw e
        }
    }

    operator fun set(id: Long, c: Client) {
        try {
            if (contains(id)) return
            val number = c.phoneNumber.ifBlank { "no number" }
            val sql = "insert into CLIENT(id, name, PHONENUMBER) values ( '$id', '${c.name}', '$number' )"
            val statement = con.createStatement()
            statement.execute(sql)
        } catch (e: Exception) {
            log.error("Set: ${e.message}")
            throw e
        }
    }

    operator fun contains(id: Long): Boolean {

        val sql = "select count(id) from client where id = '$id'"
        val statement = con.createStatement()
        val rs = statement.executeQuery(sql)

        return rs?.run {
            rs.next()
            this.getInt(1) > 0
        } ?: false
    }

    fun toList(): List<Pair<Long, Client>> {

        val sql = "select * from CLIENT"
        val statement = con.createStatement()
        val rs = statement.executeQuery(sql)

        val local = HashMap<Long, Client>()

        rs?.run {
            while(rs.next()) {
                val id = rs.getLong(1)
                local[id] = Client(id, rs.getString(2), rs.getString(3))
            }
        }

        return local.toList()
    }
}
