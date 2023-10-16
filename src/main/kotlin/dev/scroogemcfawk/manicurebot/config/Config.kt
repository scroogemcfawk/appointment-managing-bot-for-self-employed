package dev.scroogemcfawk.manicurebot.config

import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val token: String,
    val locale: String,
    val dev: ChatId,
    val manager: ChatId,
)