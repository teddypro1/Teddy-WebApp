package com.teddy.bpositive.utils

import java.time.Instant
import java.time.OffsetDateTime

/**
 * Converts Supabase ISO-8601 timestamps (including UTC offsets) to epoch milliseconds.
 * Returns null when the input is missing or cannot be parsed.
 */
fun parseCloudTimestampMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null

    return try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try {
            Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
