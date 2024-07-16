package ch.uzh.ifi.access.model.dto.chatbot

data class ContextStatusDTO(
    val successfullFiles: List<String>,
    val unsuccessfullFiles: List<String>,
    val timestamp: Int
) {
    constructor() : this(emptyList(), emptyList(),0)
}