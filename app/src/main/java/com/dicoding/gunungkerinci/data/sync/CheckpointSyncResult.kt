package com.dicoding.gunungkerinci.data.sync

import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.model.CheckpointV2Response
import com.dicoding.gunungkerinci.model.checkpointException
import retrofit2.Response

internal sealed interface CheckpointSyncResult {
    data class Sent(val serverId: Int) : CheckpointSyncResult
    data object Retry : CheckpointSyncResult
    data object Rejected : CheckpointSyncResult
    data object WaitingForSummit : CheckpointSyncResult
}

/** A successful HTTP status alone does not confirm that this event was recorded. */
internal fun classifyCheckpointResponse(
    response: Response<CheckpointV2Response>,
    log: CheckpointLogEntity,
    eventId: String
): CheckpointSyncResult {
    if (response.isSuccessful) {
        val body = response.body()
        val data = body?.data
        return if (body?.success == true && data != null &&
            data.checkpointLogId > 0 && data.phase == log.phase &&
            data.post.id == log.postId && data.pendakiId == log.pendakiId &&
            data.clientEventId == eventId && data.visitKey == "${log.phase}:${log.postId}"
        ) CheckpointSyncResult.Sent(data.checkpointLogId) else CheckpointSyncResult.Rejected
    }
    val error = response.checkpointException("Sinkronisasi check-in gagal")
    return when {
        response.code() == 409 && error.code == "CHECKPOINT_ALREADY_RECORDED" &&
            error.visitKey == "${log.phase}:${log.postId}" &&
            (error.checkpointLogId ?: 0) > 0 -> CheckpointSyncResult.Sent(error.checkpointLogId!!)
        response.code() == 409 && error.code == "DESCENT_REQUIRES_SUMMIT" ->
            CheckpointSyncResult.WaitingForSummit
        response.code() == 408 || response.code() == 429 || response.code() >= 500 ->
            CheckpointSyncResult.Retry
        response.code() == 401 -> CheckpointSyncResult.Retry
        else -> CheckpointSyncResult.Rejected
    }
}
