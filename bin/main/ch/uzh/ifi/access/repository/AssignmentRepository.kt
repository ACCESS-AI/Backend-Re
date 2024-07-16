package ch.uzh.ifi.access.repository

import ch.uzh.ifi.access.model.Assignment
import ch.uzh.ifi.access.projections.AssignmentWorkspace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter

interface AssignmentRepository : JpaRepository<Assignment?, Long?> {
    @PostFilter("filterObject.enabled and (hasRole(#courseSlug + '-assistant') or (hasRole(#courseSlug) and filterObject.isPublished) or hasRole(#courseSlug + '-supervisor'))")
    fun findByCourse_SlugOrderByOrdinalNumDesc(courseSlug: String?): List<AssignmentWorkspace>

    // TODO: implement visibility based on date
    //@PostAuthorize("hasRole(#courseSlug + '-assistant') or (hasRole(#courseSlug) and returnObject.present and returnObject.get().published)")
    @PostAuthorize("hasRole(#courseSlug + '-assistant') or (hasRole(#courseSlug))") // and returnObject.present)")
    fun findByCourse_SlugAndSlug(courseSlug: String?, assignmentSlug: String?): AssignmentWorkspace?
    fun getByCourse_SlugAndSlug(courseSlug: String?, assignmentSlug: String?): Assignment?
    fun existsByCourse_SlugAndSlug(courseSlug: String?, assignmentSlug: String?): Boolean
}