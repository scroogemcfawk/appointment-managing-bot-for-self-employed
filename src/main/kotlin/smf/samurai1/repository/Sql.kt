package smf.samurai1.repository

import org.intellij.lang.annotations.Language

object SQL {

    object CLIENT {
        @Language("SQLite")
        const val CREATE = """
            create table if not exists Client(
                id int primary key,
                name text not null,
                phoneNumber text not null
            ) strict;
        """
    }
    
    object APPOINTMENT {
        @Language("SQLite")
        const val CREATE = """
            create table if not exists Appointment(
                id int primary key,
                datetime text not null,
                client int
            ) strict;
        """
    }
}
