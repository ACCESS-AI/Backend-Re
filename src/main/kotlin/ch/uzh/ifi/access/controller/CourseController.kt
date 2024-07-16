package ch.uzh.ifi.access.controller
import ch.uzh.ifi.access.model.Submission
import ch.uzh.ifi.access.model.Task
import ch.uzh.ifi.access.model.TaskFile
import chatbot.model.Message
import ch.uzh.ifi.access.model.dto.*
import ch.uzh.ifi.access.model.dto.chatbot.ContextStatusDTO
import ch.uzh.ifi.access.model.dto.chatbot.CourseStatusDTO
import ch.uzh.ifi.access.model.dto.chatbot.PromptChatbotDTO
import ch.uzh.ifi.access.projections.*
import ch.uzh.ifi.access.service.ChatbotService
import ch.uzh.ifi.access.service.CourseService
import ch.uzh.ifi.access.service.CourseServiceForCaching
import ch.uzh.ifi.access.service.RoleService
import chatbot.model.ChatbotResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
class CourseRootController(
    private val courseService: CourseService,
) {
    @PostMapping("/create")
    @PreAuthorize("hasRole('supervisor')")
    fun createCourse(@RequestBody courseDTO: CourseDTO, authentication: Authentication): String? {
        return courseService.createCourse(courseDTO).slug
    }

    @PostMapping("/edit")
    @PreAuthorize("hasRole('supervisor')")
    fun editCourse(@RequestBody courseDTO: CourseDTO, authentication: Authentication): String? {
        return courseService.editCourse(courseDTO).slug
    }

    @PostMapping("/contact")
    fun sendMessage(@RequestBody contactDTO: ContactDTO?) {
        courseService.sendMessage(contactDTO!!)
    }

}

@RestController
@RequestMapping("/webhooks")
class WebhooksController(
    private val courseService: CourseService,
) {

    private val logger = KotlinLogging.logger {}

    @PostMapping("/courses/{course}/update/gitlab")
    fun hookGitlab(@PathVariable("course") course: String,
                     @RequestHeader("X-Gitlab-Token") secret: String) {
        logger.debug { "webhook (secret) triggered for $course"}
        courseService.webhookUpdateWithSecret(course, secret)
    }
    @PostMapping("/courses/{course}/update/github")
    fun hookGithub(@PathVariable("course") course: String,
                   @RequestHeader("X-Hub-Signature-256") signature: String,
                   @RequestBody body: String
    ) {
        logger.debug { "webhook (hmac) triggered for $course"}
        val sig = signature.substringAfter("sha256=")
        courseService.webhookUpdateWithHmac(course, sig, body)
    }

}


@RestController
@RequestMapping("/courses")
class CourseController (
    private val courseService: CourseService,
    private val courseServiceForCaching: CourseServiceForCaching,
    private val roleService: RoleService,
    private val chatbotService: ChatbotService
)
    {

    @PostMapping("/{course}/pull")
    @PreAuthorize("hasRole(#course+'-supervisor')")
    fun updateCourse(@PathVariable course: String?): String? {
        return courseService.updateCourse(course!!).slug
    }

    @GetMapping("")
    fun getCourses(): List<CourseOverview> {
        return courseService.getCoursesOverview()
    }

    @GetMapping("/{course}")
    //@PreAuthorize("hasRole(#course) or hasRole(#course+'-supervisor')")
    @PreAuthorize("hasRole(#course)")
    fun getCourseWorkspace(@PathVariable course: String?): CourseWorkspace {
        return courseService.getCourseWorkspaceBySlug(course!!)
    }

    @GetMapping("/{course}/assignments/{assignment}")
    fun getAssignment(@PathVariable course: String?, @PathVariable assignment: String?): AssignmentWorkspace {
        return courseService.getAssignment(course, assignment!!)
    }

    @GetMapping("/{course}/assignments/{assignment}/tasks/{task}/users/{user}")
    @PreAuthorize("hasRole(#course+'-assistant') or (#user == authentication.name)")
    fun getTask(
        @PathVariable course: String?,
        @PathVariable assignment: String?,
        @PathVariable task: String?,
        @PathVariable user: String?
    ): TaskWorkspace {
        return courseService.getTask(course, assignment, task, user)
    }

    @PostMapping("/{course}/assignments/{assignment}/tasks/{task}/submit")
    @PreAuthorize("hasRole(#course) and (#submission.restricted or hasRole(#course + '-assistant'))")
    fun evaluateSubmission(
        @PathVariable course: String,
        @PathVariable assignment: String,
        @PathVariable task: String?,
        @RequestBody submission: SubmissionDTO,
        authentication: Authentication
    ) {
        submission.userId = authentication.name
        // TODO: what prevents a client from sending a grading command with restricted = false?
        courseService.createSubmission(course, assignment, task!!, submission)
    }

    @GetMapping("/{courseSlug}/studentPoints")
    @PreAuthorize("hasRole(#courseSlug + '-assistant')")
    fun getStudentsWithPoints(@PathVariable courseSlug: String): List<StudentDTO> {
        return courseServiceForCaching.getStudentsWithPoints(courseSlug)
    }

    @GetMapping("/{courseSlug}/students")
    @PreAuthorize("hasRole(#courseSlug + '-assistant')")
    fun getStudents(@PathVariable courseSlug: String): List<StudentDTO> {
        return courseServiceForCaching.getStudents(courseSlug)
    }

    @GetMapping("/{courseSlug}/participants")
    fun getParticipants(@PathVariable courseSlug: String): List<StudentDTO> {
        return courseServiceForCaching.getStudents(courseSlug)
            .filter { it.email != null && it.firstName != null && it.lastName != null }
    }

    @PostMapping("/{course}/participants")
    fun registerParticipants(@PathVariable course: String, @RequestBody students: List<String>) {
        // set list of course students
        courseService.registerStudents(course, students)
        // update keycloak roles
        roleService.updateStudentRoles(courseService.getCourseBySlug(course))
    }

    @GetMapping("/{course}/participants/{participant}")
    fun getCourseProgress(@PathVariable course: String, @PathVariable participant: String): CourseProgressDTO? {
        val user = roleService.getUserByUsername(participant)?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No participant $participant")
        return courseService.getCourseProgress(course, user.username)
    }

    @GetMapping("/{course}/participants/{participant}/assignments/{assignment}")
    fun getAssignmentProgress(
        @PathVariable course: String,
        @PathVariable assignment: String,
        @PathVariable participant: String
    ): AssignmentProgressDTO? {
        val user = roleService.getUserByUsername(participant)?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No participant $participant")
        return courseService.getAssignmentProgress(course, assignment, user.username)
    }

    @GetMapping("/{course}/participants/{participant}/assignments/{assignment}/tasks/{task}")
    fun getTaskProgress(
        @PathVariable course: String, @PathVariable assignment: String,
        @PathVariable task: String, @PathVariable participant: String
    ): TaskProgressDTO? {
        val user = roleService.getUserByUsername(participant)?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No participant $participant")
        return courseService.getTaskProgress(course, assignment, task, user.username)
    }

        @GetMapping("/{course}/summary")
    fun getCourseSummary(@PathVariable course: String): CourseSummary? {
        return courseService.getCourseSummary(course)
    }

    @PostMapping("/{courseSlug}/assignments/{assignment}/tasks/{task}/users/{user}/chat/prompt")
    suspend fun promptChatbot(
          @PathVariable courseSlug: String,
          @PathVariable assignment: String,
          @PathVariable task: String,
          @PathVariable user: String,
          @RequestBody prompt: PromptChatbotDTO
      ) : ChatbotResponse
    {
        val taskWorkspace: TaskWorkspace = courseService.getTask(courseSlug, assignment, task, user)
        val taskFiles: List<TaskFile?>? = taskWorkspace.files
        val submissions: List<Submission> = courseService.getSubmissions(taskWorkspace.id, user)
        return chatbotService.promptChatbot(courseSlug, assignment, task, user, taskFiles, submissions, prompt.prompt)
    }

    @GetMapping("/{courseSlug}/assignments/{assignment}/tasks/{task}/users/{user}/chat/history")
    suspend fun getChatbotHistory(
        @PathVariable courseSlug: String,
        @PathVariable assignment: String,
        @PathVariable task: String,
        @PathVariable user: String
    ) : List<Message> {
        return chatbotService.getChatbotHistory(user, courseSlug, assignment, task)
    }
    @GetMapping("/status")
    suspend fun getContextStatus(@RequestParam("courseSlugs") courseSlugs: List<String>): List<CourseStatusDTO?> {
        return courseSlugs.map { chatbotService.getCourseContextStatus(it) }
    }
}