package smf.samurai1

import kotlinx.serialization.json.Json
import org.tinylog.Logger
import smf.samurai1.config.Config
import smf.samurai1.repository.DatabaseManager
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {

    val json = Json { ignoreUnknownKeys = true }

    val configFile = File(args.first())

    val config = try {
        json.decodeFromString(Config.serializer(), configFile.readText())
    } catch (e: Exception) {
        Logger.error{ "Failed get config: $e" }
        val configDir = Path(".").resolve("config")
        configDir.createDirectories()
        val configFile = configDir.resolve("example_config.json")
        if (configFile.notExists()) {
            configFile.createFile()
            configFile.toFile().writeText(readResourceFile("example_config.json"))
        }
        return
    }

    val con = try {
        val databaseManager = DatabaseManager(config.databaseName)
        databaseManager.getConnection()
    } catch (e: Exception) {
        Logger.error{ "Failed database initialization: $e" }
        return
    }

    try {
        val bot = Bot(config, con, configFile)

        bot.run().join()
    } catch (e: Exception) {
        Logger.error{ e.message }
    }
}
