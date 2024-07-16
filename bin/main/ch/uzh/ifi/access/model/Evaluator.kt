package ch.uzh.ifi.access.model

import ch.uzh.ifi.access.model.constants.Command
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@Entity
class Evaluator {
    @Id
    @GeneratedValue
    var id: Long? = null

    @Column(nullable = false)
    var dockerImage: String? = null

    @Column(nullable = false)
    var runCommand: String? = null

    var testCommand: String? = null

    @Column(nullable = false)
    var gradeCommand: String? = null

    @Column(nullable = false)
    var timeLimit = 30
    fun formCommand(command: Command?): String {
        return when (command) {
            Command.RUN -> runCommand!!
            Command.TEST -> testCommand!!
            Command.GRADE -> gradeCommand!!
            else -> ""  // TODO: something better?
        }
    }
}