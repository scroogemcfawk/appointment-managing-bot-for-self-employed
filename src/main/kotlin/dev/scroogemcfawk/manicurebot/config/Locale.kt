package dev.scroogemcfawk.manicurebot.config

import kotlinx.serialization.Serializable

@Serializable
data class Locale(
    val dateFormat: String,
    val dateTimeFormat: String,

    val registerUserNamePromptMessage: String,
    val registerUserPhonePromptMessage: String,
    val registerSuccessfulRegistrationMessage: String,
    val registerUserAlreadyExistsMessage: String,

    val mondayShort: String,
    val tuesdayShort: String,
    val wednesdayShort: String,
    val thursdayShort: String,
    val fridayShort: String,
    val saturdayShort: String,
    val sundayShort: String,

    val startMessage: String,
    val helpMessage: String,
    val unknownCommand: String,
)