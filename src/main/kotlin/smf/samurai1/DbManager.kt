package smf.samurai1

import org.tinylog.Logger
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.notExists

class DbManager(databaseName: String) {

    val databasePath = "/home/smf/.IdeaProjects/samurai-1/data/$databaseName"
    val con: Connection

    init {
        val path = Path(databasePath)
        path.createParentDirectories()
        if (path.notExists()) {
            path.createFile()
            Logger.info {
                "Database file created: ${path.absolutePathString()}"
            }
        }
        con = DriverManager.getConnection("jdbc:sqlite:${path.absolutePathString()}")
    }

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
