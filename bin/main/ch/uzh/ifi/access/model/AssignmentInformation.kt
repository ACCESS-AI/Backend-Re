package ch.uzh.ifi.access.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
class AssignmentInformation {
    @Id
    @GeneratedValue
    var id: Long? = null

    @JsonIgnore
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(nullable = false, name = "assignment_id")
    var assignment: Assignment? = null

    @Column(nullable = false)
    var language: String? = null

    @Column(nullable = false)
    var title: String? = null
}
