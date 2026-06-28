package com.remomo.agent.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SseEvent {
    val type: String

    @Serializable
    @SerialName("status")
    data class Status(
        override val type: String = "status",
        val status: RunStatus,
    ) : SseEvent

    @Serializable
    @SerialName("log")
    data class Log(
        override val type: String = "log",
        val message: String,
    ) : SseEvent

    @Serializable
    @SerialName("tool")
    data class Tool(
        override val type: String = "tool",
        val name: String,
        val summary: String? = null,
    ) : SseEvent

    @Serializable
    @SerialName("result")
    data class Result(
        override val type: String = "result",
        val ok: Boolean,
    ) : SseEvent

    @Serializable
    @SerialName("error")
    data class Error(
        override val type: String = "error",
        val message: String,
    ) : SseEvent
}
