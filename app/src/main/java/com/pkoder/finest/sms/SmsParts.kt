package com.pkoder.finest.sms

/** One SMS part as delivered by the platform, reduced to what parsing needs. */
data class SmsPart(
    val sender: String,
    val body: String,
    val timestampMillis: Long
)

/**
 * Bank alerts are routinely longer than 160 characters, so the platform hands us several parts of
 * the same message. Parsing each part on its own truncates the amount or produces two half
 * transactions, so parts from the same sender are stitched back together first.
 *
 * The timestamp of the first part is kept: all parts of one message share it, and it is what the
 * dedupe id is built from.
 */
fun joinSmsParts(parts: List<SmsPart>): List<SmsPart> =
    parts.groupBy { it.sender }
        .map { (sender, group) ->
            SmsPart(
                sender = sender,
                body = group.joinToString(separator = "") { it.body },
                timestampMillis = group.first().timestampMillis
            )
        }
