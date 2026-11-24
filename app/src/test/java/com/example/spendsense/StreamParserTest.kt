package com.example.spendsense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamParserTest {
    @Test fun parsesDelta() =
        assertEquals(StreamEvent.Delta("₹300 "), parseStreamLine("""{"delta": "₹300 "}"""))

    @Test fun parsesDone() = assertEquals(StreamEvent.Done, parseStreamLine("""{"done": true}"""))

    @Test fun parsesError() =
        assertEquals(StreamEvent.Error("busy"), parseStreamLine("""{"error": "busy"}"""))

    @Test fun skipsBlankAndMalformed() {
        assertNull(parseStreamLine(""))
        assertNull(parseStreamLine("not json"))
        assertNull(parseStreamLine("""{"other": 1}"""))
    }
}
