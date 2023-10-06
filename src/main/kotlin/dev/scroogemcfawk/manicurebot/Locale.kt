package dev.scroogemcfawk.manicurebot

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Locale(
    val startMessage: String,
    val helpMessage: String
)