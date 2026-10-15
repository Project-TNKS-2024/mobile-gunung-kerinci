package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.TrackingPost

internal object TrackingPostUiMapper {
    fun toCheckpointUi(
        posts: List<TrackingPost>,
        completedPostIds: Set<Int> = emptySet()
    ): List<JejakCheckpointUi> {
        val sorted = posts.sortedBy { it.urutan }
        return JejakTimelineMapper.markNext(sorted.mapIndexed { index, post ->
            val previous = sorted.getOrNull(index - 1)
            JejakCheckpointUi(
                times = listOf("—"),
                name = post.nama,
                description = JejakTimelineMapper.travelDescription(previous?.estimatedTimeMinutes),
                state = if (post.id in completedPostIds) CheckpointState.Completed else CheckpointState.Upcoming,
                postId = post.id,
                order = post.urutan,
                altitude = post.altitude,
                checkedAtLabel = if (post.id in completedPostIds) "Sudah check-in" else null,
                estimateIsDummy = previous?.infoIsDummy == true
            )
        })
    }
}
