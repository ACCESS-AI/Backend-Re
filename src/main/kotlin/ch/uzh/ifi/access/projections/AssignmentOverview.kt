package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Assignment
import ch.uzh.ifi.access.model.AssignmentInformation
import ch.uzh.ifi.access.model.dao.Timer
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection
import java.time.LocalDateTime

@Projection(types = [Assignment::class])
interface AssignmentOverview {
    val id: Long?
    val slug: String?

    @get:Value("#{'Assignment ' + target.ordinalNum.toString()}")
    val name: String?
    val information: Map<String?, AssignmentInformation?>?
    val startDate: LocalDateTime?
    val endDate: LocalDateTime?
    val countDown: List<Timer?>?
    val isPastDue: Boolean
    val isActive: Boolean
    @get:Value("#{@courseService.calculateAssignmentMaxPoints(target.tasks, null)}")
    val maxPoints: Double?

    @get:Value("#{@courseService.calculateAssignmentPoints(target.tasks, null)}")
    val points: Double?

    @get:Value("#{target.tasks.size()}")
    val tasksCount: Int?
}
