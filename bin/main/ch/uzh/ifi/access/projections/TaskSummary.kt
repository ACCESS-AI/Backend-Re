package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Task
import org.springframework.data.rest.core.config.Projection

@Projection(types = [Task::class])
interface TaskSummary {
    val slug: String?
    val ordinalNum: Int?
    val maxPoints: Double?
    val information: Map<String?, TaskInformationPublic?>?
    val maxAttempts: Int?
    val attemptRefill: Int?
    val dockerImage: String?
    val runCommand: String?
    val testCommand: String?
    val gradeCommand: String?
    val timeLimit: Int?
}