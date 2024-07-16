package ch.uzh.ifi.access.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*


@Entity
class ResultFile {
    @Id
    @GeneratedValue
    var id: Long? = null

    @Column(nullable = false)
    var path: String? = null

    @JsonIgnore
    @ManyToOne
    @JoinColumn(nullable = false, name = "submission_id")
    var submission: Submission? = null

    @Column(nullable = false)
    var mimeType: String? = null

    @Column(nullable=true, columnDefinition="text")
    var content: String? = null

    @Column(nullable=true, columnDefinition="bytea")
    var contentBinary: ByteArray? = null

    val binary: Boolean
        get() = contentBinary != null

}