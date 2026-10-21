package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.TrackingPost

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
    val campingStatus: String,
    val temperatureText: String = "—",
    val distanceToSummitText: String = "—",
    val mapImageUrl: String? = null
) {
    companion object {
        val Default = LocationDetailUi(
            positionLabel = "Lokasi belum tersedia",
            postLabel = "",
            placeName = "Lokasi belum tersedia",
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

        // Jarak dan durasi antarpos berasal dari backend, termasuk saat memakai cache offline.
        val sortedPosts = allPosts.sortedBy { it.urutan }
        val currentIndex = sortedPosts.indexOfFirst { it.id == checkpoint.postId || it.urutan == checkpoint.order }
        val nextPost = if (currentIndex in 0 until sortedPosts.lastIndex) sortedPosts[currentIndex + 1] else null
        val currentPost = if (currentIndex >= 0) sortedPosts[currentIndex] else null

        // v2: segmen keluar sudah dinormalisasi arah itinerary (turun memakai segmen mundur).
        val isV2 = checkpoint.visitKey != null
        val distanceMeters = if (isV2) {
            checkpoint.distanceToNextM?.takeIf { it >= 0 }
        } else {
            currentPost?.distanceToNextM?.takeIf { it >= 0 && nextPost != null }
        }
        val estimateMinutes = if (isV2) {
            checkpoint.estimatedTimeToNextMinutes?.takeIf { it >= 0 }
        } else {
            currentPost?.estimatedTimeMinutes?.takeIf { it >= 0 && nextPost != null }
        }

        val isLastPost = currentIndex == sortedPosts.lastIndex && sortedPosts.isNotEmpty()
        val isRouteEnd = isV2 && checkpoint.phase == "turun" && currentIndex == 0

        val distanceText = if (distanceMeters != null) {
            if (distanceMeters >= 1000) {
                String.format(java.util.Locale("id", "ID"), "%.1f km", distanceMeters / 1000.0)
            } else {
                "$distanceMeters m"
            }
        } else if (isV2 && checkpoint.isSummit) {
            "Puncak"
        } else if (isRouteEnd) {
            "Titik Akhir"
        } else if (isLastPost) {
            "Puncak / Titik Akhir"
        } else {
            "-"
        }

        val estimateText = if (estimateMinutes != null) {
            if (estimateMinutes >= 60) {
                "${estimateMinutes / 60} jam ${estimateMinutes % 60} mnt"
            } else {
                "$estimateMinutes Menit"
            }
        } else if (isV2 && checkpoint.isSummit) {
            "Puncak Tertinggi"
        } else if (isLastPost) {
            "Puncak Tertinggi"
        } else {
            "-"
        }

        val nextDirectedPost = when {
            !isV2 -> nextPost
            checkpoint.phase == "turun" || checkpoint.isSummit -> sortedPosts.getOrNull(currentIndex - 1)
            else -> nextPost
        }
        val distanceLabel = when {
            nextDirectedPost != null -> "Jarak menuju ${nextDirectedPost.nama}"
            else -> "Jarak menuju pos selanjutnya"
        }

        val altitudeValue = checkpoint.altitude ?: currentPost?.altitude
        val altitudeFormatted = if (altitudeValue != null && altitudeValue > 0) "$altitudeValue Mdpl" else "- Mdpl"

        return LocationDetailUi(
            mapImageUrl = currentPost?.mapImageUrl,
            positionLabel = currentPost?.nama?.takeIf { it.isNotBlank() }
                ?: checkpoint.name.takeIf { it.isNotBlank() }
                ?: "Lokasi belum tersedia",
            postLabel = postLabel,
            placeName = placeName,
            altitudeText = altitudeFormatted,
            distanceToNextLabel = distanceLabel,
            distanceToNextValue = distanceText,
            averageEstimateValue = estimateText,
            statusText = when (checkpoint.state) {
                CheckpointState.Completed -> "Sudah check-in"
                CheckpointState.Current -> "Checkpoint selanjutnya"
                CheckpointState.Upcoming -> "Belum check-in"
            },
            checkpointDescription = checkpoint.description,
            mountainStatus = "Normal",
            waterSourceStatus = "Tersedia",
            campingStatus = if (postNumber in listOf(1, 3, 4)) "Boleh" else "Tidak Direkomendasikan",
            temperatureText = currentPost?.temperatureC?.takeIf { it.isFinite() }?.let {
                String.format(java.util.Locale.forLanguageTag("id-ID"), "%.1f °C", it)
            } ?: "—",
            distanceToSummitText = currentPost?.distanceToSummitM?.takeIf { it >= 0 }?.let {
                if (it >= 1000) {
                    String.format(java.util.Locale.forLanguageTag("id-ID"), "%.1f km", it / 1000.0)
                } else "$it m"
            } ?: "—"
        )
    }

    private fun extractPostNumber(name: String): Int? =
        Regex("(?i)pos\\s+(\\d+)").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
}
