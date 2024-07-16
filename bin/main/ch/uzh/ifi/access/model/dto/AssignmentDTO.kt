package ch.uzh.ifi.access.model.dto

import com.fasterxml.jackson.annotation.JsonFormat
import lombok.Data
import java.time.LocalDateTime

@Data
class AssignmentDTO(
    var slug: String? = null,
    var ordinalNum: Int? = null,
    var description: String? = null,
    var information: MutableMap<String, AssignmentInformationDTO> = HashMap(),

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    var start: LocalDateTime? = null,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    var end: LocalDateTime? = null,
    var tasks: MutableList<String> = ArrayList()
)
