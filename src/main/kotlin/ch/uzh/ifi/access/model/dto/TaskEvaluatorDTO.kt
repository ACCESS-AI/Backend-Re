package ch.uzh.ifi.access.model.dto

import lombok.Data

@Data
class TaskEvaluatorDTO(
    var dockerImage: String? = null,
    var runCommand: String? = null,
    var testCommand: String? = null,
    var gradeCommand: String? = null,
    var gradeResults: String? = null,
    var timeLimit: Int? = null,
    var resources: MutableList<String> = ArrayList()
)
