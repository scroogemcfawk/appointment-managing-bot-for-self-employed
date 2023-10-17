package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.abstracts.WithChat
import java.time.LocalDateTime

fun LocalDateTime.isFuture(): Boolean {
    return this > LocalDateTime.now()
}

val WithChat.chatId: Long
    get() = this.chat.id.chatId