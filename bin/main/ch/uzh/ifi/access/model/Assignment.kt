package ch.uzh.ifi.access.model

import ch.uzh.ifi.access.model.dao.Timer
import jakarta.persistence.*
import org.hibernate.annotations.OrderBy
import java.time.Duration
import java.time.LocalDateTime

@Entity
class Assignment {
    @Id
    @GeneratedValue
    var id: Long? = null

    @Column(nullable = false)
    var slug: String? = null

    @Column(nullable = false)
    var ordinalNum: Int? = null

    @OneToMany(mappedBy = "assignment", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @MapKey(name = "language")
    var information: MutableMap<String, AssignmentInformation> = HashMap()

    @Column(name = "start_date")
    var start: LocalDateTime? = null

    @Column(name = "end_date")
    var end: LocalDateTime? = null

    // assignments which are not enabled are not referenced by a course config
    // it could be that the assignments slug was changed
    var enabled = false

    @ManyToOne
    @JoinColumn(nullable = false, name = "course_id")
    var course: Course? = null

    @OneToMany(mappedBy = "assignment", cascade = [CascadeType.ALL])
    @OrderBy(clause = "ID ASC")
    var tasks: MutableList<Task> = ArrayList()

    @Transient
    var points: Double? = null

    val isPublished: Boolean
        get() = start!!.isBefore(LocalDateTime.now())

    val isPastDue: Boolean
        get() = end!!.isBefore(LocalDateTime.now())

    val isActive: Boolean
        get() = isPublished && !isPastDue

    val countDown: List<Timer>
        get() {
            val remaining = Duration.between(LocalDateTime.now(), end)
            return listOf(
                Timer("DAYS", remaining.toDays(), Duration.between(start, end).toDays()),
                Timer("HOURS", remaining.toHoursPart().toLong(), 24L),
                Timer("MINUTES", remaining.toMinutesPart().toLong(), 60L)
            )
        }

    fun createTask(): Task {
        val newTask = Task()
        tasks.add(newTask)
        newTask.assignment = this
        return newTask
    }
}