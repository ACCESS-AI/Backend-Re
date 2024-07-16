package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Course
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection

@Projection(types = [Course::class])
interface CourseWorkspace : CourseOverview {
    @get:Value("#{@courseService.getAssignments(target.slug)}")
    val assignments: List<AssignmentWorkspace?>?
}