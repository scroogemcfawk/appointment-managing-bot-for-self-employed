package smf.samurai1.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import smf.samurai1.readResourceFile
import smf.samurai1.repository.DatabaseManager
import java.nio.file.Path
import java.sql.Connection
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

class ConfigManager(
    args: Array<String>
) {

    val config: Config
    val locale: Locale
    val connection: Connection

    init {
        var configPath: Path? = null

        config = deserializeAndGetOrWriteFromResource(
            Config.serializer(), "example_config.json", "Failed to initialize config"
        ) {
            configPath = Path(args.first())
            configPath
        }

        locale = deserializeAndGetOrWriteFromResource(
            Locale.serializer(), "example_locale.json", "Failed to initialize locale"
        ) {
            configPath!!.parent.resolve(config.locale)
        }

        connection = try {
            val databaseManager = DatabaseManager(config.databaseName)
            databaseManager.getConnection()
        } catch (e: Exception) {
            throw Exception("Failed to get database connection", e)
        }
    }

    fun <R, T : KSerializer<R>> deserializeAndGetOrWriteFromResource(
        serializer: T, resourceFilename: String, onFailMessage: String, pathResolver: () -> Path
    ): R {
        return try {
            json.decodeFromString(serializer, pathResolver().toFile().readText())
        } catch (e: Exception) {
            val configDir = Path(".").resolve("config")
            configDir.createDirectories()
            val configFile = configDir.resolve(resourceFilename)
            if (configFile.notExists()) {
                configFile.createFile()
                configFile.toFile().writeText(readResourceFile(resourceFilename))
            }
            throw Exception(onFailMessage, e)
        }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
