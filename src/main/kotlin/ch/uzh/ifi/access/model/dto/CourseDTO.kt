package ch.uzh.ifi.access.model.dto

import lombok.Data
import java.time.LocalDateTime

@Data
class CourseDTO (
    var slug: String? = null,
    var repository: String? = null,
    var repositoryUser: String? = null,
    var repositoryPassword: String? = null,
    var webhookSecret: String? = null,
    var logo: String? = null,
    var information: MutableMap<String, CourseInformationDTO> = HashMap(),
    var defaultVisibility: String? = null,
    var overrideVisibility: String? = null,
    var overrideStart: LocalDateTime? = null,
    var overrideEnd: LocalDateTime? = null,
    var studentRole: String? = null,
    var assistantRole: String? = null,
    var supervisorRole: String? = null,
    var supervisors: MutableSet<MemberDTO> = mutableSetOf(),
    var assistants: MutableSet<MemberDTO> = mutableSetOf(),
    var assignments: MutableList<String> = ArrayList(),
    var globalFiles: CourseFilesDTO? = null
)
