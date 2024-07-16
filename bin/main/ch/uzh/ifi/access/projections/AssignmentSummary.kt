package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Assignment
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection
import java.time.LocalDateTime

@Projection(types = [Assignment::class])
interface AssignmentSummary {
    val slug: String?
    val information: Map<String?, AssignmentInformationPublic?>?
    val start: LocalDateTime?
    val end: LocalDateTime?
    @get:Value("#{@courseService.calculateAssignmentMaxPoints(target.tasks, null)}")
    val maxPoints: Double?
    @get:Value("#{@courseService.enabledTasksOnly(target.tasks)}")
    val tasks: List<TaskSummary?>?
    val ordinalNum: Int?
}