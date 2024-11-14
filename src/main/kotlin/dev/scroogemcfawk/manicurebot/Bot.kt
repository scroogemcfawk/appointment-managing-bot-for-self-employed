package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.scroogemcfawk.manicurebot.callbacks.CallbackHandler
import dev.scroogemcfawk.manicurebot.commands.CommandHandler
import dev.scroogemcfawk.manicurebot.config.Config
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.Appointment
import dev.scroogemcfawk.manicurebot.domain.AppointmentList
import dev.scroogemcfawk.manicurebot.domain.Client
import dev.scroogemcfawk.manicurebot.domain.ClientList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import org.tinylog.Logger
import java.io.File
import java.sql.Connection
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

class Bot(private val config: Config, con: Connection) {

    private val locale = try {
        val ignoringKeys = Json {
            ignoreUnknownKeys = true
        }
        ignoringKeys.decodeFromString(Locale.serializer(), File(config.locale).readText())
    } catch (e: Exception) {
        throw Exception("Failed locale deserialization: ${e.message}")
    }

    private val bot = telegramBot(config.token)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val appointments = AppointmentList(locale.dateTimeFormat, con)
    private val clientChats = ClientList(con)

    init {
        try {
            config.manager ?.let {
                clientChats[it] = Client(it, "Contractor", "-")
            }
            config.dev ?.let {
                clientChats[it] = Client(it, "Developer", "-")
            }
        } catch (e: Exception) {
            throw Exception("Failed manager chats initialization: ${e.message}")
        }
//        mock()
    }

    @Suppress("SpellCheckingInspection", "unused")
    private fun mock() {
        fun getLDTAfterMinutes(m: Int): LocalDateTime {
            val dtin2m = LocalDateTime.now().plusMinutes(m.toLong())
            val ldin2m = dtin2m.toLocalDate()
            val ltin2m = LocalTime.of(dtin2m.hour, dtin2m.minute)
            return LocalDateTime.of(ldin2m, ltin2m)
        }
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 21, 11, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 26, 12, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 27, 13, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 28, 17, 45), null))

//        appointments.add(Appointment(getLDTAfterMinutes(1), null))
//        appointments.add(Appointment(getLDTAfterMinutes(2), null))
//        appointments.add(Appointment(getLDTAfterMinutes(3), null))
//        appointments.add(Appointment(getLDTAfterMinutes(4), null))
    }

    private suspend fun logBotRunningMessage() {
        val me = bot.getMe()
        Logger.info {
            "${me.username?.username} (${me.firstName}) is running."
        }
    }

    @OptIn(PreviewFeature::class, RiskFeature::class)
    suspend fun run(): Job = bot.buildBehaviourWithLongPolling(scope) {
        val commandHandler = CommandHandler(this, config, locale, clientChats)
        // IDEA: refactor this shit to local catch with ctx.waitCallbackQueries<DataCallbackQuery>()
        val callbackHandler = CallbackHandler(this, config, locale, clientChats, appointments)

        onText {
            Logger.info {
                it.text
            }
        }

        //=================================== COMMON ===========================================

        onCommand(locale.startCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.start(msg)
        }

        onCommand(locale.helpCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.help(msg)
        }

        onCommand(locale.registerCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.register(msg)
        }

        onCommand(locale.idCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.id(msg)
        }

        onUnhandledCommand { msg ->
            commandHandler.unhandled(msg)
        }

        //=============== CLIENT COMMANDS ==============================

        onCommand(locale.appointmentCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.appointment(msg, appointments)
        }

        onCommand(locale.rescheduleCommand, requireOnlyCommandInMessage = true) {msg->
            commandHandler.reschedule(msg, appointments)
        }

        onCommand(locale.cancelCommand, requireOnlyCommandInMessage = true) {msg->
            commandHandler.cancel(msg, appointments)
        }

        //=============== CONTRACTOR COMMANDS ==============================

        onCommand(locale.addCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.add(msg)
        }

        onCommand(locale.listCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.list(msg, appointments)
        }

        onCommand(locale.deleteCommand, requireOnlyCommandInMessage = true) {msg ->
            commandHandler.delete(msg, appointments)
        }

        onCommand(locale.notifyCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.notify(msg)
        }

        //=============== CALLBACKS ==============================

        // TODO: rewrite this with onDataCallbackQuery(String) so it's the same as command handling

        onDataCallbackQuery { cb ->
            callbackHandler.processCallback(cb, appointments)
        }

        logBotRunningMessage()
    }
}
