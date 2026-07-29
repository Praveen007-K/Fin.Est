package com.pkoder.finest.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsPartsTest {

    @Test
    fun `joins multipart messages from the same sender in order`() {
        val parts = listOf(
            SmsPart("SBIINB", "Dear customer, Rs.1,2", 1_000L),
            SmsPart("SBIINB", "50.00 debited from A/C XX1234", 1_000L)
        )

        val joined = joinSmsParts(parts)

        assertEquals(1, joined.size)
        assertEquals("Dear customer, Rs.1,250.00 debited from A/C XX1234", joined.first().body)
        assertEquals(1_000L, joined.first().timestampMillis)
    }

    @Test
    fun `a joined multipart message parses as one transaction`() {
        val parts = listOf(
            SmsPart("SBIINB", "Your A/C XX1234 debited by Rs.1,2", 5_000L),
            SmsPart("SBIINB", "50.75 on 01Sep25 trf to store@okaxis", 5_000L)
        )

        val joined = joinSmsParts(parts)
        val parsed = joined.mapNotNull { SmsParser.parse(it.sender, it.body, it.timestampMillis) }

        assertEquals(1, parsed.size)
        assertEquals(1250.75, parsed.first().amount, 0.001)
    }

    @Test
    fun `messages from different senders stay separate`() {
        val parts = listOf(
            SmsPart("SBIINB", "Rs.10 debited", 1L),
            SmsPart("HDFCBK", "Rs.20 credited", 2L)
        )

        val joined = joinSmsParts(parts)

        assertEquals(2, joined.size)
    }
}
