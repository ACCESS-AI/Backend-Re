package ch.uzh.ifi.access.repository

import ch.uzh.ifi.access.model.Course
import ch.uzh.ifi.access.projections.CourseOverview
import ch.uzh.ifi.access.projections.CourseSummary
import ch.uzh.ifi.access.projections.CourseWorkspace
import ch.uzh.ifi.access.projections.MemberOverview
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.security.access.prepost.PostFilter

interface CourseRepository : JpaRepository<Course?, Long?> {
    fun getBySlug(courseSlug: String?): Course?
    fun findBySlug(courseSlug: String?): CourseWorkspace?

    @PostFilter("hasRole(filterObject.slug)")
    fun findCoursesBy(): List<CourseOverview>

    @PostFilter("hasRole(filterObject.slug)")
    fun findCoursesByAndDeletedFalse(): List<CourseOverview>

    fun findAllByDeletedFalse(): List<Course>

    @Query(
        nativeQuery = true, value = "SELECT a.value AS name, :email AS email FROM user_attribute a " +
                "WHERE a.name='displayName' AND a.user_id=(SELECT e.id FROM user_entity e WHERE e.email=:email)"
    )
    fun getTeamMemberName(email: String?): MemberOverview?
    fun findCourseBySlug(courseSlug: String?): CourseSummary?

    @Query(
        nativeQuery = true, value = """
            SELECT sum(e.best_score) AS total_points
            FROM evaluation e
            JOIN task t ON e.task_id = t.id
            JOIN assignment a ON t.assignment_id = a.id
            JOIN course c ON a.course_id = c.id
            WHERE e.id IN (
                SELECT MAX(id)
                FROM evaluation
                WHERE user_id = ANY(:userIds)
                GROUP BY task_id
            )
            AND c.slug = :courseSlug
            AND e.user_id = ANY(:userIds)
        """
    )
    fun getTotalPoints(courseSlug: String, userIds: Array<String>): Double?

}