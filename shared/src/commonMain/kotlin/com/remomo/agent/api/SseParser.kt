package com.remomo.agent.api

import com.remomo.agent.api.dto.SseEvent
import com.remomo.agent.api.dto.RunStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SseParser {
    fun parseEvent(data: String, json: Json = ApiJson): SseEvent {
        val element = json.parseToJsonElement(data)
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
            ?: error("SSE payload missing type")
        return when (type) {
            "status" -> json.decodeFromJsonElement(
                SseEvent.Status.serializer(),
                element,
            )
            "log" -> json.decodeFromJsonElement(SseEvent.Log.serializer(), element)
            "tool" -> json.decodeFromJsonElement(SseEvent.Tool.serializer(), element)
            "result" -> json.decodeFromJsonElement(SseEvent.Result.serializer(), element)
            "error" -> json.decodeFromJsonElement(SseEvent.Error.serializer(), element)
            else -> error("Unknown SSE event type: $type")
        }
    }

    fun parseStatusOnly(data: String, json: Json = ApiJson): RunStatus? {
        val element = json.parseToJsonElement(data) as? JsonObject ?: return null
        if (element["type"]?.jsonPrimitive?.content != "status") return null
        return json.decodeFromJsonElement(SseEvent.Status.serializer(), element).status
    }
}
