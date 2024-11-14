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
                instant text not null,
                client integer null
            ) strict;
        """
    }
}
