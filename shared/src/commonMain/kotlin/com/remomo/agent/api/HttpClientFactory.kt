package com.remomo.agent.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

expect fun createPlatformHttpClient(): HttpClient

fun createHttpClient(): HttpClient =
    createPlatformHttpClient().config {
        install(ContentNegotiation) {
            json(ApiJson)
        }
        expectSuccess = false
    }
