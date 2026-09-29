package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.TrackingPost

internal object TrackingPostUiMapper {
    fun toCheckpointUi(
        posts: List<TrackingPost>,
        completedPostIds: Set<Int> = emptySet()
    ): List<JejakCheckpointUi> =
        posts.sortedBy { it.urutan }.map { post ->
            JejakCheckpointUi(
                times = listOf("Post ${post.urutan}"),
                name = post.nama,
                description = buildDescription(post),
                state = if (post.id in completedPostIds) CheckpointState.Completed else CheckpointState.Upcoming,
                postId = post.id,
                order = post.urutan,
                altitude = post.altitude
            )
        }

    private fun buildDescription(post: TrackingPost): String {
        val details = mutableListOf(
            "Lat ${post.latitude}, Long ${post.longitude}"
        )
        post.altitude?.let { details += "Alt $it mdpl" }
        post.radiusMeter?.let { details += "Radius $it m" }
        return details.joinToString(" • ")
    }
}
