package ch.uzh.ifi.access.repository

import ch.uzh.ifi.access.model.SubmissionFile
import org.springframework.data.jpa.repository.JpaRepository

interface SubmissionFileRepository : JpaRepository<SubmissionFile?, Long?> {
    fun findTopByTaskFile_IdAndSubmission_UserIdOrderByIdDesc(
        taskFileId: Long?,
        userId: String?
    ): SubmissionFile?
}