package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.scroogemcfawk.manicurebot.callbacks.CallbackHandler
import dev.scroogemcfawk.manicurebot.commands.CommandHandler
import dev.scroogemcfawk.manicurebot.config.Config
import dev.scroogemcfawk.manicurebot.config.Locale
import dev.scroogemcfawk.manicurebot.domain.Appointment
import dev.scroogemcfawk.manicurebot.domain.AppointmentList
import dev.scroogemcfawk.manicurebot.domain.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

class Bot(private val config: Config) {

    private val locale = Json.decodeFromString(Locale.serializer(), File(config.locale).readText())
    private val bot = telegramBot(config.token)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val log = LoggerFactory.getLogger(Bot::class.java)

    private val appointments = AppointmentList(locale.dateTimeFormat)
    private val userChats = HashMap<Long, User>()

    init {
        userChats[config.manager.chatId] = User(config.manager.chatId, "Contractor", "")
        userChats[config.dev.chatId] = User(config.dev.chatId, "Developer", "")
        mock()
    }

    @Suppress("SpellCheckingInspection", "unused")
    private fun mock() {
        fun getLDTAfterMinutes(m: Int): LocalDateTime {
            val dtin2m = LocalDateTime.now().plusMinutes(m.toLong())
            val ldin2m = dtin2m.toLocalDate()
            val ltin2m = LocalTime.of(dtin2m.hour, dtin2m.minute)
            return LocalDateTime.of(ldin2m, ltin2m)
        }
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 22, 11, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 23, 12, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 24, 13, 30), null))
        appointments.add(Appointment(LocalDateTime.of(2023, Month.OCTOBER, 25, 17, 45), null))

//        appointments.add(Appointment(getLDTAfterMinutes(1), null))
//        appointments.add(Appointment(getLDTAfterMinutes(2), null))
//        appointments.add(Appointment(getLDTAfterMinutes(3), null))
//        appointments.add(Appointment(getLDTAfterMinutes(4), null))
    }

    private suspend fun logBotRunningMessage() {
        val me = bot.getMe()
        log.info(
            "${me.username?.username} (${me.firstName}) is running."
        )
    }

    @OptIn(PreviewFeature::class)
    suspend fun run(): Job = bot.buildBehaviourWithLongPolling(scope) {
        val commandHandler = CommandHandler(this, config, locale, userChats)
        // IDEA: refactor this shit to local catch with ctx.waitCallbackQueries<DataCallbackQuery>()
        val callbackHandler = CallbackHandler(this, config, locale, userChats, appointments)

        //=================================== COMMON ===========================================

        onCommand(locale.startCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.start(msg)
        }

        onCommand(locale.helpCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.help(msg)
        }

        onCommand(locale.registerCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.register(msg, userChats)
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

        onCommand(locale.notifyCommand, requireOnlyCommandInMessage = true) { msg ->
            commandHandler.notify(msg, userChats)
        }

        //=============== CALLBACKS ==============================

        onDataCallbackQuery { cb ->
            callbackHandler.processCallback(cb, appointments)
        }

        logBotRunningMessage()
    }
}
