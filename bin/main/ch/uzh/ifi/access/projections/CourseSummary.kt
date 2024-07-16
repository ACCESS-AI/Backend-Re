package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Course
import ch.uzh.ifi.access.model.dto.StudentDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection
import java.time.LocalDateTime

@Projection(types = [Course::class])
interface CourseSummary {

    val slug: String?
    val overrideStart: LocalDateTime?
    val overrideEnd: LocalDateTime?
    val information: Map<String?, CourseInformationPublic?>?

    @get:Value("#{@courseService.getMaxPoints(target.slug)}")
    val maxPoints: Double?
    val studentsCount: Long?

    @get:Value("#{@courseService.getTeamMembers(target.supervisors)}")
    val supervisors: Set<MemberOverview?>?

    @get:Value("#{@courseService.getTeamMembers(target.assistants)}")
    val assistants: Set<MemberOverview?>?

    @get:Value("#{@courseServiceForCaching.getStudentsWithPoints(target.slug)}")
    val students: List<StudentDTO?>?
    val assignments: List<AssignmentSummary?>?

}