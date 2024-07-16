package ch.uzh.ifi.access.model.dto

import lombok.Data

@Data
class ContactDTO(
    var name: String? = null,
    var email: String? = null,
    var message: String? = null,
    var topic: String? = null
) {
    fun formatContent(): String {
        return "Name: $name\nEmail: $email\nTopic: $topic\nMessage: $message"
    }
}
