package dev.scroogemcfawk.manicurebot.config

import kotlinx.serialization.Serializable

@Serializable
data class Locale(
    // default values as fallback
    val isDefaultLocale: Boolean = true,
    val dateFormat: String = "yyyy.MM.dd",
    val dateTimeFormat: String = "yyyy.MM.dd HH:mm",

    val startCommand: String = "start",
    val helpCommand: String = "help",
    val registerCommand: String = "register",
    val idCommand: String = "_id",
    val appointmentCommand: String = "app",
    val addCommand: String = "add",
    val listCommand: String = "list",
    val deleteCommand: String = "delete",
    val notifyCommand: String = "notify",
    val cancelCommand: String = "cancel",
    val rescheduleCommand: String = "reschedule",

    val rescheduleCommandShort: String = "r",
    val deleteCommandShort: String = "d",
    val rescheduleContractorShowAppointmentsToRescheduleToAction: String = "1",

    val registerUserNamePromptMessage: String = "Please, enter your name:",
    val registerUserPhonePromptMessage: String = "Please, enter your phone number:",
    val registerSuccessfulRegistrationMessageTemplate: String = "Done. You can now signup for appointments.",
    val registerUserAlreadyExistsMessage: String = "You are already registered.",

    val rescheduleDoneMessageTemplate: String = "Your appointment rescheduled from $1 to $2.",
    val rescheduleYouDontHaveAppointmentMessage: String = "You do not have any appointments.",
    val rescheduleChooseAppointmentToRescheduleToPromptMessage: String =
        "Choose an appointment to reschedule to:",
    val rescheduleNoAppointmentsAvailableMessage: String = "No appointments available.",
    val rescheduleContractorChooseAppointmentToReschedulePromptMessage: String =
        "Choose an appointment you want to reschedule:",

    val appointmentNotRegisteredMessageTemplate: String = "You are not registered. Please register using /$1 command.",
    val appointmentAlreadyTakenMessage: String = "Appointment is not available.",
    val appointmentAppointmentsNotAvailableMessage: String = "Sorry, appointments are not available.",
    val appointmentAlreadyHaveAppointmentMessage: String = "Sorry, you already have an appointment: ",
    val appointmentChooseAppointmentMessage: String = "Please, choose an appointment: ",

    val cancelNoAppointmentsFoundMessageTemplate: String = "You don't have any appointments scheduled.",
    val cancelConfirmMessageTemplate: String = "Your appointment is on $1, are you sure you want " +
            "to cancel it?",
    val cancelDoneMessage: String = "Appointment canceled.",
    val cancelContractorChoosePromptMessage: String = "Choose an appointment to cancel:",

    val operationCancelledMessage: String = "Operation cancelled.",

    val addSelectDayPromptMessage: String = "Please, select a day: ",

    val listNoAppointmentsMessage: String = "No appointments",

    val deleteSelectAppointmentPromptMessage: String = "Select an appointment to delete:",
    val deleteSuccessMessage: String = "Appointment has been deleted.",

    val notifyNotificationMessagePromptMessage: String = "Please, enter your notification message: ",

    val cbInvalidCallbackNotificationMessage: String = "Invalid callback.",

    val cbAddSelectTimePromptMessage: String = "Select time:",
    val cbAddTimeAppointmentCreatedTemplate: String = "Created appointment $1.",

    val cbCalendarSwitchingNotificationMessage: String = "Switching to",

    val cbAppointmentCompleteMessage: String = "You have an appointment on $1",
    val cbAppointmentCompleteManagerNotificationMessage: String = "You have and appointment on $1 with $2",

    val remindUserMessageTemplate: String = "You have an appointment on $1 at $2.",

    val address: String = "Planet Earth",

    val appointmentHasBeenCanceledMessage: String = "Appointment has been canceled.",
    val appointmentHasBeenCanceledTemplate: String = "Appointment $1 has been canceled.",

    val featureIsNotImplementedYetMessage: String = "This feature is not implemented yet.",

    val mondayShort: String = "Mon",
    val tuesdayShort: String = "Tue",
    val wednesdayShort: String = "Wed",
    val thursdayShort: String = "Thu",
    val fridayShort: String = "Fri",
    val saturdayShort: String = "Sat",
    val sundayShort: String = "Sun",

    val defaultLocaleMessage: String = "If you see this message, please text @scroogemcfawk asap.",

    val startMessageTemplate: String = defaultLocaleMessage,
    val helpMessage: String = defaultLocaleMessage,
    val helpContractorMessage: String = defaultLocaleMessage,
    val unknownCommand: String = defaultLocaleMessage,
)
