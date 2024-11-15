package smf.samurai1.repository

import org.intellij.lang.annotations.Language

object SQL {

    object CLIENT {
        @Language("SQLite")
        const val CREATE = """
            create table if not exists Client(
                id integer primary key,
                name text not null,
                phoneNumber text not null
            ) strict;
        """

        @Language("SQLite")
        const val SELECT_BY_ID = "select * from Client where id = ?;"

        @Language("SQLite")
        const val SELECT_ALL = "select * from Client;"

        @Language("SQLite")
        const val INSERT = "insert into Client(id, name, phoneNumber) values (?, ?, ?);"

        @Language("SQLite")
        const val COUNT = "select count(id) from Client where id = ?;"
    }

    object APPOINTMENT {
        @Language("SQLite")
        const val CREATE = """
            create table if not exists Appointment(
                id integer primary key,
                datetime text not null,
                client integer null
            ) strict;
        """

        @Language("SQLite")
        const val SELECT_ALL = "select * from Appointment;"

        @Language("SQLite")
        const val SELECT_ALL_IN_FUTURE = "select * from Appointment where datetime > datetime();"

        @Language("SQLite")
        const val DELETE_ALL_IN_PAST = "delete from Appointment where datetime < datetime();"

        @Language("SQLite")
        const val INSERT = "insert into Appointment(datetime, client) values(?, ?);"

        @Language("SQLite")
        const val CANCEL = "update Appointment set client = null where datetime = ? and client = ?;"

        @Language("SQLite")
        const val DELETE = "delete from Appointment where datetime = ?;"

        @Language("SQLite")
        const val ASSIGN_CLIENT = "update Appointment set client = ? where datetime = ?;"
    }
}
