package dev.scroogemcfawk.manicurebot.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val token: String,
    val locale: String,
    val dev: Long? = null,
    val manager: Long? = null,
    val notifyClientBeforeAppointmentInHours: Int = 24,
    val dataSourceUrl: String,
)
