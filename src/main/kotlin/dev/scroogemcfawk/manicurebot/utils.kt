package dev.scroogemcfawk.manicurebot

import dev.inmo.tgbotapi.abstracts.WithChat
import dev.inmo.tgbotapi.types.ChatId
import java.time.LocalDateTime

fun LocalDateTime.isFuture(): Boolean {
    return this > LocalDateTime.now()
}

val WithChat.chatId: Long
    get() = this.chat.id.chatId

val Long.chatId: ChatId
    get() = ChatId(this)
