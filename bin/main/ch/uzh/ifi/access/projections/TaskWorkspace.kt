package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Submission
import ch.uzh.ifi.access.model.Task
import ch.uzh.ifi.access.model.TaskFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection
import java.time.LocalDateTime

@Projection(types = [Task::class])
interface TaskWorkspace : TaskOverview {
    val isTestable: Boolean

    @get:Value("#{@courseService.getTaskFiles(target.id, target.userId)}")
    val files: List<TaskFile?>?

    @get:Value("#{@courseService.getSubmissions(target.id, target.userId)}")
    val submissions: List<Submission?>?

    @get:Value("#{@courseService.getNextAttemptAt(target.id, target.userId)}")
    val nextAttemptAt: LocalDateTime?

    @get:Value("#{@courseService.getAssignmentDeadlineForTask(target.id)}")
    val deadline: LocalDateTime?
}
