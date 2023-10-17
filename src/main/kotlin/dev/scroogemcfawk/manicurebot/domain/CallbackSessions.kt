package dev.scroogemcfawk.manicurebot.domain

import java.util.*
import kotlin.collections.HashMap

class CallbackSessions {
    // chatId -> stack of arguments for current action
    private val userChatSession = HashMap<Long, Stack<String>>()

    fun clear(id: Long) {
        userChatSession[id] = Stack()
    }


    operator fun get(id: Long): Stack<String> {
        userChatSession[id] ?. let { return it }
        userChatSession[id] = Stack()
        return userChatSession[id]!!
    }

    operator fun set(id: Long, value: String) {
        if (id !in userChatSession) {
            userChatSession[id] = Stack()
        }
        userChatSession[id]!!.push(value)
    }
}