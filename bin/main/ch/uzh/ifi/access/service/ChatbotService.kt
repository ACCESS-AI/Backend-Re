package ch.uzh.ifi.access.service
import ch.uzh.ifi.access.model.Submission
import ch.uzh.ifi.access.model.TaskFile
import ch.uzh.ifi.access.model.dto.chatbot.ContextStatusDTO
import ch.uzh.ifi.access.model.dto.chatbot.CourseStatusDTO
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.core.env.Environment
import java.nio.file.Path
import chatbot.model.Chatbot
import chatbot.model.ChatbotResponse
import chatbot.model.Message
import java.security.MessageDigest
import com.fasterxml.jackson.databind.ObjectMapper


@Configuration
class ChatbotServiceConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}

data class ContextData(
    val course: String,
    val slug: String
)

data class StatusRequest(
    val slug: String
)

@Service
class ChatbotService(
    private val restTemplate: RestTemplate,
    private val env: Environment,
    private val objectMapper: ObjectMapper) {
    
    fun createContext(slug: String, coursePath: Path) {
        //this should be changed and put in a config file
        val chatbotApiUrl = env.getProperty("CHATBOT_CONTEXT_SERVICE_URL") + "/contexts/create"
        val headers = HttpHeaders()
        headers.set("Content-Type", "application/json")
        val courseSlugHash = hashSlug(slug)
        val contextData = ContextData(course = coursePath.fileName.toString(), slug = courseSlugHash)
        val requestEntity = HttpEntity(contextData, headers)
        restTemplate.exchange(chatbotApiUrl, HttpMethod.PUT, requestEntity, String::class.java)
    }

    fun getCourseContextStatus(slug: String) : CourseStatusDTO?{
        var context: ContextStatusDTO?
        try {
            val contextServiceUrl = env.getProperty("CHATBOT_CONTEXT_SERVICE_URL")
            val endpoint = "/contexts/course_slug/status"
            val url = "$contextServiceUrl$endpoint".replace("course_slug", hashSlug(slug))
            val headers = HttpHeaders()
            headers.set("Content-Type", "application/json")
            val responseEntity = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String::class.java)
            context = responseEntity.body?.let { objectMapper.readValue(it, ContextStatusDTO::class.java) }
        } catch(e: Exception){
            context = ContextStatusDTO();
        }

        return CourseStatusDTO(slug,context)
    }

    private fun getTaskInstructionsString(taskFiles: List<TaskFile?>?): String {
        val taskInstructionFiles: List<TaskFile?>? = taskFiles?.filter { it?.instruction == true }
        val taskInstructionsString = StringBuilder()
        taskInstructionFiles?.forEach { taskInstructionsString.append("${it?.template ?: ""}\n") }
        return taskInstructionsString.toString()
    }

    private fun getSubmissionFilesString(submissions: List<Submission>): List<String> {
        val submissionStrings: MutableList<String> = mutableListOf()
        submissions.forEach { submission ->
        val submissionString = StringBuffer()
            submission.files.forEach { file ->
                submissionString.append("${file.taskFile?.name}:\n")
                submissionString.append("${file.content}\n\n")
            }
        submissionStrings.add(submissionString.toString())
        }
        return submissionStrings
    }

    suspend fun promptChatbot(courseSlug: String, assignment: String, task: String, user: String, taskInstructions: List<TaskFile?>?, submissions: List<Submission>, prompt: String): ChatbotResponse {
        val courseSlugHash: String = hashSlug(courseSlug)

        val taskInstructionsString: String = getTaskInstructionsString(taskInstructions)

        val submissionStrings: List<String> = getSubmissionFilesString(submissions)

        val chatbot = Chatbot(user, courseSlug, courseSlugHash, assignment, task)
        return chatbot.run(taskInstructionsString, submissionStrings, prompt)
    }

    suspend fun getChatbotHistory(user: String, courseSlug: String, assignment: String, task: String) : List<Message>{
        val courseSlugHash = hashSlug(courseSlug)
        val chatbot = Chatbot(user, courseSlug, courseSlugHash, assignment, task)
        return chatbot.getHistory()
    }

    private fun hashSlug(slug: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(slug.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .filter(Char::isLetter)
            .take(20)
}
