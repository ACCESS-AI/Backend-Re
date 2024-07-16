package ch.uzh.ifi.access.model.dto

import lombok.Data

@Data
class TaskFilesDTO(
    var instruction: List<String> = ArrayList(),
    var visible: List<String> = ArrayList(),
    var editable: List<String> = ArrayList(),
    var grading: List<String> = ArrayList(),
    var solution: List<String> = ArrayList(),
    var persist: List<String> = ArrayList()
)

class CourseFilesDTO : TaskFilesDTO()