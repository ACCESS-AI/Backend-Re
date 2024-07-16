package ch.uzh.ifi.access.repository

import ch.uzh.ifi.access.model.Submission
import ch.uzh.ifi.access.model.constants.Command
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.security.access.prepost.PostFilter
import java.time.LocalDateTime

interface SubmissionRepository : JpaRepository<Submission?, Long?> {
    @PostFilter("not filterObject.graded or hasRole(filterObject.evaluation.task.assignment.course.slug + '-assistant')")
    fun findByEvaluation_Task_IdAndUserId(taskId: Long?, userId: String?): List<Submission>

    @PostFilter("not hasRole(filterObject.evaluation.task.assignment.course.slug + '-assistant')")
    fun findByEvaluation_Task_IdAndUserIdAndCommand(
        taskId: Long?,
        userId: String?,
        command: Command?
    ): List<Submission>

    @Query("SELECT DISTINCT s.userId FROM Submission s WHERE s.evaluation.task.assignment.course.id=:courseId AND s.createdAt > :start")
    fun countOnlineByCourse(courseId: Long?, start: LocalDateTime?): List<String>
}