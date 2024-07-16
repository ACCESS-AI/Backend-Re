package ch.uzh.ifi.access.model

import ch.uzh.ifi.access.model.constants.Command
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import lombok.Getter
import lombok.Setter
import org.hibernate.annotations.OrderBy
import java.time.Duration
import java.time.LocalDateTime

@Getter
@Setter
@Entity
class Evaluation {
    @Id
    @GeneratedValue
    var id: Long? = null

    @Column(nullable = false)
    var userId: String? = null
    var bestScore: Double? = null
    var remainingAttempts: Int? = null

    @JsonIgnore
    @ManyToOne
    @JoinColumn(nullable = false, name = "task_id")
    var task: Task? = null

    @OneToMany(mappedBy = "evaluation", cascade = [CascadeType.ALL])
    @OrderBy(clause = "CREATED_AT DESC")
    var submissions: MutableList<Submission> = ArrayList()

    @Transient
    var nextAttemptAt: LocalDateTime? = null
    val isActive: Boolean
        get() = task!!.assignment!!.isActive

    fun countSubmissionsByType(command: Command): Long {
        return submissions.stream().filter { submission: Submission -> submission.command == command }.count()
    }

    fun addSubmission(newSubmission: Submission): Submission {
        submissions.add(newSubmission)
        newSubmission.evaluation = this
        newSubmission.ordinalNum = countSubmissionsByType(newSubmission.command!!)
        return newSubmission
    }

    fun update(newScore: Double?) {
        remainingAttempts = remainingAttempts!! - 1
        if (isActive) {
            bestScore = (bestScore ?: 0.0).coerceAtLeast(newScore!!)
        }
    }

    @PostLoad
    fun updateRemainingAttempts() {
        task?.attemptWindow.let {
            submissions
                .filter { it.isGraded && it.valid }
                .map {
                    it.createdAt
                }.firstOrNull {
                    it?.isBefore(
                        LocalDateTime.now()
                    ) ?: false
                }?.let { createdAt ->
                    task?.let {
                        val refills = Duration.between(createdAt, LocalDateTime.now()).dividedBy(
                            it.attemptWindow
                        )
                        it.maxAttempts?.let { maxAttempts ->
                            remainingAttempts?.let { remainingAttempts ->
                                if (maxAttempts - remainingAttempts <= refills) this.remainingAttempts =
                                    maxAttempts else {
                                    this.remainingAttempts = remainingAttempts + refills.toInt()
                                    it.attemptWindow?.let { attemptWindow ->
                                        nextAttemptAt = createdAt.plus(attemptWindow.multipliedBy(refills + 1))
                                    }
                                }
                            }
                        }
                    }

                }
        }
    }
}