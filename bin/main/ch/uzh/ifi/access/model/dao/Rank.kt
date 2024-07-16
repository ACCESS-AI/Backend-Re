package ch.uzh.ifi.access.model.dao

import lombok.Data

@Data
class Rank(var email: String, avgScore: Double, avgAttemptsCount: Double) {
    var evaluationId: Long? = null
    var score: Double = avgScore - avgAttemptsCount / 10.0
}
