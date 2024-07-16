package ch.uzh.ifi.access.model.dto

import lombok.Data
import lombok.NoArgsConstructor

@Data
@NoArgsConstructor
class SubmissionFileDTO(
    var taskFileId: Long? = null,
    var content: String? = null
)
