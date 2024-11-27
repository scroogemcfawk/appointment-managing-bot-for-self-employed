package smf.samurai1

import org.tinylog.Logger
import smf.samurai1.config.ConfigManager

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {

    // todo fix signal propagation for child process in start script

    val configManager = runCatching {
        ConfigManager(args)
    }.getOrElse{
        Logger.error { it.message }
        return
    }

    try {
        Bot(configManager).run().join()
    } catch (e: Exception) {
        Logger.error { e }
    }
}
