package ch.uzh.ifi.access.model.dto

import lombok.Data

@Data
class CourseInformationDTO(
    var language: String? = null,
    var title: String? = null,
    var repository: String? = null,
    var description: String? = null,
    var university: String? = null,
    var period: String? = null,
)
