package ch.uzh.ifi.access.model.dto

import lombok.Data
import lombok.NoArgsConstructor

@Data
@NoArgsConstructor
class TaskFileDTO {
    var path: String? = null
    var template: String? = null
    var templateBinary: ByteArray? = null
    var mimeType: String? = null
}
