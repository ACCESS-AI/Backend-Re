package ch.uzh.ifi.access.model.constants

import com.fasterxml.jackson.annotation.JsonValue
import lombok.Getter
import java.util.*

@Getter
enum class Command {
    RUN,
    TEST,
    GRADE;

    @get:JsonValue
    val jsonName: String
        get() = this.name.lowercase(Locale.getDefault())
    val displayName: String
        get() = if (this == GRADE) "Submission" else name.replaceFirstChar(Char::titlecase)
    val isGraded: Boolean
        get() = this == GRADE
}