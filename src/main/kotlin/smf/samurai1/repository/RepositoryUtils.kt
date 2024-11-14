package smf.samurai1.repository

import java.sql.Connection
import java.sql.PreparedStatement

fun Connection.prepareStatement(sql: String, fieldSetter: PreparedStatement.() -> Unit): PreparedStatement {
    val statement = this.prepareStatement(sql)

    statement.fieldSetter()

    return statement
}
