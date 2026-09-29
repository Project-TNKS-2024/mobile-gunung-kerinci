package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.TrackingPost
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal data class LocationDetailUi(
    val positionLabel: String,
    val postLabel: String,
    val placeName: String,
    val altitudeText: String,
    val distanceToNextLabel: String,
    val distanceToNextValue: String,
    val averageEstimateValue: String,
    val statusText: String,
    val checkpointDescription: String,
    val mountainStatus: String,
    val waterSourceStatus: String,
    val campingStatus: String
) {
    companion object {
        val Default = LocationDetailUi(
            positionLabel = "POS 3",
            postLabel = "Pos 3:",
            placeName = "Pondok Panorama",
            altitudeText = "- Mdpl",
            distanceToNextLabel = "Jarak menuju pos selanjutnya",
            distanceToNextValue = "-",
            averageEstimateValue = "-",
            statusText = "Belum check-in",
            checkpointDescription = "Checkpoint belum tersedia",
            mountainStatus = "Normal",
            waterSourceStatus = "Tersedia",
            campingStatus = "Boleh"
        )
    }
}

internal object LocationDetailUiMapper {
    fun fromCheckpoint(
        checkpoint: JejakCheckpointUi?,
        allPosts: List<TrackingPost> = emptyList()
    ): LocationDetailUi {
        if (checkpoint == null) return LocationDetailUi.Default

        val postNumber = checkpoint.order ?: checkpoint.postId ?: extractPostNumber(checkpoint.name) ?: 0
        val nameParts = checkpoint.name.split(":", limit = 2)
        val postLabel = if (nameParts.size == 2) {
            "${nameParts[0].trim()} :"
        } else {
            checkpoint.name
        }
        val placeName = if (nameParts.size == 2) {
            nameParts[1].trim()
        } else {
            checkpoint.name
        }

        // Kalkulasi jarak & estimasi ke pos berikutnya berdasarkan daftar pos
        val sortedPosts = allPosts.sortedBy { it.urutan }
        val currentIndex = sortedPosts.indexOfFirst { it.id == checkpoint.postId || it.urutan == checkpoint.order }
        val nextPost = if (currentIndex in 0 until sortedPosts.lastIndex) sortedPosts[currentIndex + 1] else null
        val currentPost = if (currentIndex >= 0) sortedPosts[currentIndex] else null

        val distanceMeters = if (currentPost != null && nextPost != null) {
            calculateDistanceMeters(
                currentPost.latitude.toDoubleOrNull() ?: 0.0,
                currentPost.longitude.toDoubleOrNull() ?: 0.0,
                nextPost.latitude.toDoubleOrNull() ?: 0.0,
                nextPost.longitude.toDoubleOrNull() ?: 0.0
            )
        } else null

        val (distanceText, estimateText) = if (distanceMeters != null) {
            val distString = if (distanceMeters >= 1000) {
                String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000.0)
            } else {
                "${distanceMeters.roundToInt()} m"
            }
            // Estimasi pendakian gunung rata-rata ~2 km/jam (±30 m/menit)
            val estimateMinutes = (distanceMeters / 33.3).roundToInt().coerceAtLeast(10)
            val estString = if (estimateMinutes >= 60) {
                "${estimateMinutes / 60} jam ${estimateMinutes % 60} mnt"
            } else {
                "$estimateMinutes Menit"
            }
            distString to estString
        } else if (currentIndex == sortedPosts.lastIndex && sortedPosts.isNotEmpty()) {
            "Puncak / Titik Akhir" to "Puncak Tertinggi"
        } else {
            "-" to "-"
        }

        val altitudeValue = checkpoint.altitude ?: currentPost?.altitude
        val altitudeFormatted = if (altitudeValue != null && altitudeValue > 0) "$altitudeValue Mdpl" else "- Mdpl"

        return LocationDetailUi(
            positionLabel = if (postNumber > 0) "POS $postNumber" else checkpoint.name.uppercase(),
            postLabel = postLabel,
            placeName = placeName,
            altitudeText = altitudeFormatted,
            distanceToNextLabel = if (nextPost != null) "Jarak menuju ${nextPost.nama}" else "Jarak menuju pos selanjutnya",
            distanceToNextValue = distanceText,
            averageEstimateValue = estimateText,
            statusText = when (checkpoint.state) {
                CheckpointState.Completed -> "Sudah check-in"
                CheckpointState.Current -> "Posisi saat ini"
                CheckpointState.Upcoming -> "Belum check-in"
            },
            checkpointDescription = checkpoint.description,
            mountainStatus = "Normal",
            waterSourceStatus = "Tersedia",
            campingStatus = if (postNumber in listOf(1, 3, 4)) "Boleh" else "Tidak Direkomendasikan"
        )
    }

    private fun extractPostNumber(name: String): Int? =
        Regex("(?i)pos\\s+(\\d+)").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()

    /** Haversine formula dalam meter */
    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meter
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
