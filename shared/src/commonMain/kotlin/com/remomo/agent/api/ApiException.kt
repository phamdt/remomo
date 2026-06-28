package com.remomo.agent.api

import com.remomo.agent.api.dto.RunStatus

class ApiException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)

fun RunStatus.isTerminal(): Boolean =
    this == RunStatus.COMPLETED || this == RunStatus.FAILED || this == RunStatus.CANCELLED

fun RunStatus.isActive(): Boolean =
    this == RunStatus.QUEUED || this == RunStatus.RUNNING
