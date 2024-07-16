package ch.uzh.ifi.access.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
class CourseInformation {
    @Id
    @GeneratedValue
    var id: Long? = null

    @JsonIgnore
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(nullable = false, name = "course_id")
    var course: Course? = null

    @Column(nullable = false)
    var language: String? = null

    @Column(nullable = false)
    var title: String? = null

    @Column(nullable = false)
    var description: String? = null

    @Column(nullable = false)
    var university: String? = null

    @Column(nullable = false)
    var period: String? = null
}
