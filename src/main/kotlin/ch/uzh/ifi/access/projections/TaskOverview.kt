package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Task
import ch.uzh.ifi.access.model.TaskInformation
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection

@Projection(types = [Task::class])
interface TaskOverview {
    val id: Long?
    val ordinalNum: Int?
    val slug: String?

    @get:Value("#{'Task ' + target.ordinalNum.toString()}")
    val name: String?
    val information: Map<String?, TaskInformation?>?
    val maxPoints: Double?
    val maxAttempts: Int?
    fun setUserId(userId: String?)

    @get:Value("#{target.assignment.active}")
    val isActive: Boolean

    @get:Value("#{@courseService.calculateAvgTaskPoints(target.slug)}")
    val avgPoints: Double?

    @get:Value("#{@courseService.calculateTaskPoints(target.id, target.userId)}")
    val points: Double?

    @get:Value("#{@courseService.getRemainingAttempts(target.id, target.userId, target.maxAttempts)}")
    val remainingAttempts: Int?
}