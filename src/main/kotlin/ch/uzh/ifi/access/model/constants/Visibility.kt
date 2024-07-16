package ch.uzh.ifi.access.model.constants

import com.fasterxml.jackson.annotation.JsonValue
import lombok.AllArgsConstructor
import lombok.Getter
import java.util.*

@Getter
@AllArgsConstructor
enum class Visibility {
    HIDDEN,
    REGISTERED,
    PUBLIC;

    @get:JsonValue
    val jsonName: String
        get() = name.lowercase(Locale.getDefault())
}
