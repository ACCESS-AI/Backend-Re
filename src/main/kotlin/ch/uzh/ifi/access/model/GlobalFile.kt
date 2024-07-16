package ch.uzh.ifi.access.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
class GlobalFile {
    @Id
    @GeneratedValue
    var id: Long? = null

    @Column(nullable = false)
    var path: String? = null

    @Column(nullable = false)
    var name: String? = null

    @Column(nullable = false)
    var mimeType: String? = null

    @JsonIgnore
    @ManyToOne
    @JoinColumn(nullable = false, name = "course_id")
    var course: Course? = null

    @Column(nullable = false, columnDefinition = "text")
    var template: String? = null

    @Column(nullable=true, columnDefinition="bytea")
    var templateBinary: ByteArray? = null

    val binary: Boolean
        get() = templateBinary != null

    @Transient
    private val content: String? = null
    var enabled = false


    @Column(nullable = false)
    var visible = false

    @Column(nullable = false)
    var editable = false

    @Column(nullable = false)
    var grading = false

    @Column(nullable = false)
    var solution = false

    @Column(nullable = false)
    var instruction = false

    //val isPublished: Boolean
    //    get() = !grading && instruction || visible || (solution && task?.assignment?.isPastDue?: false)

}