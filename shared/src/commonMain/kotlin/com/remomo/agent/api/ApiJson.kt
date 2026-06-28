package com.remomo.agent.api

import kotlinx.serialization.json.Json

val ApiJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
