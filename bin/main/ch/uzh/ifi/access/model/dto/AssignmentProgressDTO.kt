package ch.uzh.ifi.access.model.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class AssignmentProgressDTO(
    val userId: String? = null,
    val slug: String? = null,
    val information: MutableMap<String, AssignmentInformationDTO> = mutableMapOf(),
    val tasks: List<TaskProgressDTO>? = null
)

