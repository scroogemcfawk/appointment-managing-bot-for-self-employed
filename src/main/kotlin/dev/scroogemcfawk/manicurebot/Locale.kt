package dev.scroogemcfawk.manicurebot

import kotlinx.serialization.Serializable

@Serializable
data class Locale(
    val startMessage: String,
    val helpMessage: String,

    val unknownCommand: String,

    val mondayShort: String,
    val tuesdayShort: String,
    val wednesdayShort: String,
    val thursdayShort: String,
    val fridayShort: String,
    val saturdayShort: String,
    val sundayShort: String,
)