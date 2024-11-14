package smf.samurai1.repository

import org.tinylog.Logger
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.notExists

class DatabaseManager(databaseName: String) {

    val databasePath = "/home/smf/.IdeaProjects/samurai-1/data/$databaseName"
    val path: Path = Path(databasePath)

    init {
        path.createParentDirectories()
        if (path.notExists()) {
            path.createFile()
            Logger.info {
                "Database file created: ${path.absolutePathString()}"
            }
        }
    }

    fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:${path.absolutePathString()}")
    }
}
