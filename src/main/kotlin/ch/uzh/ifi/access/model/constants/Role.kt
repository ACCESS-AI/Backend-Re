package ch.uzh.ifi.access.model.constants

import com.google.common.base.Joiner
import java.util.*

enum class Role {
    STUDENT,
    ASSISTANT,
    SUPERVISOR;

    val jsonName: String
        get() = name.lowercase(Locale.getDefault())

    /*TODO: this doesn't seem safe. What if someone manages to create a course "something-supervisor"
            then this person would become supervisor of the course called "something" */
    fun withCourse(courseSlug: String?): String {
        return Joiner.on("-").skipNulls().join(courseSlug, jsonName)
    }

    val subRole: Role?
        get() = if (this == SUPERVISOR) ASSISTANT else null
}
