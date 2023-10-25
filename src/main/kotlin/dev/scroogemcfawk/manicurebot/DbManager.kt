package dev.scroogemcfawk.manicurebot

import java.sql.Connection
import java.sql.DriverManager

class DbManager(dataBaseUrl: String) {
    val con: Connection = DriverManager.getConnection(dataBaseUrl)

    private fun createClientTable() {
        val sql = """
            create table if not exists client(
                id bigint primary key,
                name varchar(64) not null,
                phoneNumber varchar(16) not null
            )
        """.trimIndent()
        val statement = con.createStatement()
        statement.execute(sql)
    }

    private fun createAppointmentTable() {
        val sql = """
            create table if not exists appointment(
                id bigint auto_increment primary key,
                datetime datetime not null,
                client bigint
            )
        """.trimIndent()
        val statement = con.createStatement()
        statement.execute(sql)
    }

    @Suppress("UNUSED", "SqlWithoutWhere")
    fun dropData() {
        val sql = "delete from APPOINTMENT"
        val statement = con.createStatement()
        statement.execute(sql)
    }

    fun initDataBase() {
        createAppointmentTable()
        createClientTable()
    }
}
