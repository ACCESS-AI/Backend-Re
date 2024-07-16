package ch.uzh.ifi.access.projections

import ch.uzh.ifi.access.model.Course
import ch.uzh.ifi.access.model.CourseInformation
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection
import java.time.LocalDateTime

@Projection(types = [Course::class])
interface CourseOverview {
    val id: Long?
    val slug: String?
    val logo: String?
    val information: Map<String?, CourseInformation?>?
    val overrideStart: LocalDateTime?
    val overrideEnd: LocalDateTime?

    @get:Value("#{@courseService.calculateCoursePoints(target.assignments, null)}")
    val points: Double?

    @get:Value("#{@courseService.getMaxPoints(target.slug)}")
    val maxPoints: Double?

    @get:Value("#{@roleService.getOnlineCount(target.slug)}")
    val onlineCount: Long?
    val studentsCount: Long?

    @get:Value("#{@courseService.getTeamMembers(target.supervisors)}")
    val supervisors: Set<MemberOverview>?

    @get:Value("#{@courseService.getTeamMembers(target.assistants)}")
    val assistants: Set<MemberOverview>?
}