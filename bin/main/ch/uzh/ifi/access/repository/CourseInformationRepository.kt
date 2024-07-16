package ch.uzh.ifi.access.repository

import ch.uzh.ifi.access.model.CourseInformation
import org.springframework.data.jpa.repository.JpaRepository

interface CourseInformationRepository : JpaRepository<CourseInformation?, Long?>
