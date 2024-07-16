package ch.uzh.ifi.access.model.dao

import lombok.Data

@Data
class Results(
    var points: Double? = null,
    var hints: MutableList<String> = mutableListOf()
)