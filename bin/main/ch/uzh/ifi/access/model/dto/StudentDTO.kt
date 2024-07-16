package ch.uzh.ifi.access.model.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class StudentDTO(
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var points: Double? = null,
    var username: String? = null,
    var registrationId: String? = null
)
