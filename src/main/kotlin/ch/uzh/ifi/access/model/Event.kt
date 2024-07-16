package ch.uzh.ifi.access.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonGetter
import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity
class Event {
    @Id
    @GeneratedValue
    var id: Long? = null
    var type: String? = null

    @Column(nullable = false)
    var description: String? = null

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    var date: LocalDateTime? = null

    @ManyToOne
    @JoinColumn(nullable = false, name = "course_id")
    var course: Course? = null

    @get:JsonGetter
    val time: String
        get() = date!!.format(DateTimeFormatter.ofPattern("HH:mm"))
}