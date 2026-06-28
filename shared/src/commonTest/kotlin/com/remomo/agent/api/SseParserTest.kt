package com.remomo.agent.api

import com.remomo.agent.api.dto.RunStatus
import com.remomo.agent.api.dto.SseEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SseParserTest {
    @Test
    fun parsesStatusEvent() {
        val event = SseParser.parseEvent("""{"type":"status","status":"running"}""")
        assertEquals(SseEvent.Status(status = RunStatus.RUNNING), event)
    }

    @Test
    fun parsesLogEvent() {
        val event = SseParser.parseEvent("""{"type":"log","message":"Hello agent"}""")
        assertEquals(SseEvent.Log(message = "Hello agent"), event)
    }

    @Test
    fun parsesToolEvent() {
        val event = SseParser.parseEvent("""{"type":"tool","name":"grep","summary":"src/"}""")
        assertEquals(SseEvent.Tool(name = "grep", summary = "src/"), event)
    }

    @Test
    fun rejectsUnknownEventType() {
        assertFailsWith<IllegalStateException> {
            SseParser.parseEvent("""{"type":"assistant","message":"nope"}""")
        }
    }
}
