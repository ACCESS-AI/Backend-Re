package ch.uzh.ifi.access.model.dto

import lombok.Data

@Data
class AssignmentInformationDTO(
    var language: String? = null,
    var title: String? = null
)
