package ch.uzh.ifi.access.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
class TaskInformation {
    @Id
    @GeneratedValue
    var id: Long? = null

    @JsonIgnore
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(nullable = false, name = "task_id")
    var task: Task? = null

    @Column(nullable = false)
    var language: String? = null

    @Column(nullable = false)
    var title: String? = null

    @Column(nullable = false)
    var instructionsFile: String? = null
}
