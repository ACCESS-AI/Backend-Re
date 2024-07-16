package ch.uzh.ifi.access.model.dao

import lombok.Data

@Data
class Timer (
    var name: String? = null,
    var current: Long? = null,
    var max: Long? = null
)
