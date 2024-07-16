package ch.uzh.ifi.access.projections
import ch.uzh.ifi.access.model.Evaluation
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection

@Projection(types = [Evaluation::class])
interface EvaluationSummary {
    val userId: String?

    @get:Value("#{target.task.slug}")
    val url: String?

    @get:Value("#{target.task.maxAttempts}")
    val maxAttempts: String?

    @get:Value("#{target.task.maxPoints}")
    val maxPoints: String?
    val bestScore: Double?
    val remainingAttempts: Int?
    val submissions: List<SubmissionSummary>
}

