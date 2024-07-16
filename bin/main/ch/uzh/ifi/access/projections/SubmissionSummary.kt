package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Submission
import ch.uzh.ifi.access.model.constants.Command
import org.springframework.data.rest.core.config.Projection
import java.time.LocalDateTime


@Projection(types = [Submission::class])
interface SubmissionSummary {
    val ordinalNum: Int?
    val points: Double?
    val isValid: Boolean
    val command: Command?
    val output: String?
    val createdAt: LocalDateTime?
    val files: List<Any?>?
    val persistentResultFiles: List<Any?>?
}

