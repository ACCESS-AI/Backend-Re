package ch.uzh.ifi.access.model.dto.chatbot

data class CourseStatusDTO(
    val courseSlug: String,
    val status: ContextStatusDTO?
)
