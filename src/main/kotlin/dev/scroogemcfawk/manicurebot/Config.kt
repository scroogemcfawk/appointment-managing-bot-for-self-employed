package dev.scroogemcfawk.manicurebot

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val token: String
)