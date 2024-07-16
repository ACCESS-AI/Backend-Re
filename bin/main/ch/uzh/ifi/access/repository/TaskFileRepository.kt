package ch.uzh.ifi.access.repository

import ch.uzh.ifi.access.model.TaskFile
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.access.prepost.PostFilter

interface TaskFileRepository : JpaRepository<TaskFile?, Long?> {
    @Transactional
    @PostFilter("hasRole(filterObject.task.assignment.course.slug + '-assistant') or filterObject.isPublished")
    fun findByTask_IdAndEnabledTrueOrderByIdAscPathAsc(taskId: Long?): List<TaskFile>

    @Transactional
    fun findByTask_IdAndEnabledTrue(taskId: Long?): List<TaskFile>
    fun findByTask_IdAndPath(taskId: Long?, filePath: String?): TaskFile?
}