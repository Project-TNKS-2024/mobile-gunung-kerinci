package com.dicoding.gunungkerinci.ui.pelacakan_jejak

internal data class CheckpointQrPayload(val postId: Int)

internal object CheckpointQrParser {
    private const val PREFIX = "TNKS_CHECKPOINT:"

    // Cocok dengan: tnks://checkpoint?post_id=5  (atau query param lain sebelumnya)
    private val URL_REGEX = Regex(
        """^tnks://checkpoint\?.*post_id=(\d+)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(rawValue: String?): CheckpointQrPayload? {
        val raw = rawValue?.trim().orEmpty()
        if (raw.isBlank()) return null

        // Format 1: TNKS_CHECKPOINT:3
        if (raw.startsWith(PREFIX, ignoreCase = true)) {
            return raw.substringAfter(":")
                .trim()
                .toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let(::CheckpointQrPayload)
        }

        // Format 2: tnks://checkpoint?post_id=5
        URL_REGEX.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?.let { return CheckpointQrPayload(it) }

        return null
    }
}
