package ch.uzh.ifi.access.service

import ch.uzh.ifi.access.model.*
import ch.uzh.ifi.access.model.constants.Command
import ch.uzh.ifi.access.model.dao.Rank
import ch.uzh.ifi.access.model.dao.Results
import ch.uzh.ifi.access.model.dto.*
import ch.uzh.ifi.access.projections.*
import ch.uzh.ifi.access.repository.*
import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.HostConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import jakarta.xml.bind.DatatypeConverter
import org.apache.commons.collections4.ListUtils
import org.apache.commons.io.FileUtils
import org.apache.tika.Tika
import org.keycloak.representations.idm.UserRepresentation
import org.modelmapper.ModelMapper
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.http.HttpStatus
import org.springframework.lang.Nullable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.RoundingMode
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.stream.Stream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class CourseServiceForCaching(
    private val roleService: RoleService,
    private val courseService: CourseService,
    private val courseRepository: CourseRepository,
) {

    fun getStudents(courseSlug: String): List<StudentDTO> {
        val course = courseService.getCourseBySlug(courseSlug)
        return course.registeredStudents.map {
            val user = roleService.getUserByUsername(it)
            if (user != null) {
                val studentDTO = courseService.getStudent(courseSlug, user)
                studentDTO.username = user.username
                studentDTO.registrationId = it
                studentDTO
            } else {
                StudentDTO(registrationId = it)
            }
        }
    }

    fun getStudentsWithPoints(courseSlug: String): List<StudentDTO> {
        val course = courseService.getCourseBySlug(courseSlug)
        return course.registeredStudents.map {
            val user = roleService.getUserByUsername(it)
            if (user != null) {
                // TODO!: make sure evaluations are saved under only a single user ID in the future!
                // for now, retrieve all possible user IDs from keycloak and retrieve all matching evaluations
                val userIds = roleService.getAllUserIdsFor(user.username)
                val coursePoints = courseRepository.getTotalPoints(courseSlug, userIds.toTypedArray()) ?: 0.0
                val studentDTO = StudentDTO(user.firstName, user.lastName, user.email, coursePoints.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble(), user.username, it)
                studentDTO
            } else {
                StudentDTO(registrationId = it)
            }
        }
    }

}

// TODO: decide properly which parameters should be nullable
@Service
class CourseService(
    private val workingDir: Path,
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val taskRepository: TaskRepository,
    private val taskFileRepository: TaskFileRepository,
    private val submissionRepository: SubmissionRepository,
    private val submissionFileRepository: SubmissionFileRepository,
    private val evaluationRepository: EvaluationRepository,
    private val dockerClient: DockerClient,
    private val modelMapper: ModelMapper,
    private val jsonMapper: JsonMapper,
    private val courseLifecycle: CourseLifecycle,
    private val roleService: RoleService,
    private val fileService: FileService,
    private val tika: Tika
) {

    private val logger = KotlinLogging.logger {}

    private fun verifyUserId(@Nullable userId: String?): String {
        return userId ?: SecurityContextHolder.getContext().authentication.name
    }

    fun getCourseBySlug(courseSlug: String): Course {
        return courseRepository.getBySlug(courseSlug) ?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No course found with the URL $courseSlug")
    }
    fun getCourseWorkspaceBySlug(courseSlug: String): CourseWorkspace {
        return courseRepository.findBySlug(courseSlug) ?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No course found with the URL $courseSlug")
    }

    fun getTaskById(taskId: Long): Task {
        return taskRepository.findById(taskId).get() ?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No task found with the ID $taskId")
    }

    fun getTaskFileById(fileId: Long): TaskFile {
        return taskFileRepository.findById(fileId).get() ?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No task file found with the ID $fileId")
    }
    fun getCoursesOverview(): List<CourseOverview> {
        //return courseRepository.findCoursesBy()
        return courseRepository.findCoursesByAndDeletedFalse()
    }

    fun getCourses(): List<Course> {
        //return courseRepository.findCoursesBy()
        return courseRepository.findAllByDeletedFalse()
    }

    fun getCourseSummary(courseSlug: String): CourseSummary {
        return courseRepository.findCourseBySlug(courseSlug) ?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No course found with the URL $courseSlug")
    }

    fun enabledTasksOnly(tasks: List<Task>): List<Task> {
        return tasks.filter { it.enabled }
    }

    // TODO: clean up these confusing method names
    fun getAssignments(courseSlug: String?): List<AssignmentWorkspace> {
        return assignmentRepository.findByCourse_SlugOrderByOrdinalNumDesc(courseSlug)
    }

    fun getAssignment(courseSlug: String?, assignmentSlug: String): AssignmentWorkspace {
        return assignmentRepository.findByCourse_SlugAndSlug(courseSlug, assignmentSlug) ?:
            throw ResponseStatusException( HttpStatus.NOT_FOUND,
                    "No assignment found with the URL $assignmentSlug" )
    }

    fun getAssignmentBySlug(courseSlug: String?, assignmentSlug: String): Assignment {
        return assignmentRepository.getByCourse_SlugAndSlug(courseSlug, assignmentSlug) ?:
        throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No assignment found with the URL $assignmentSlug"
            )
    }

    fun getTask(courseSlug: String?, assignmentSlug: String?, taskSlug: String?, userId: String?): TaskWorkspace {
        val workspace =
            taskRepository.findByAssignment_Course_SlugAndAssignment_SlugAndSlug(courseSlug, assignmentSlug, taskSlug) ?:
            throw ResponseStatusException( HttpStatus.NOT_FOUND,
                        "No task found with the URL: $courseSlug/$assignmentSlug/$taskSlug" )
        workspace.setUserId(userId)
        return workspace
    }

    fun getTaskFiles(taskId: Long?, userId: String?): List<TaskFile> {
        val permittedFiles = taskFileRepository.findByTask_IdAndEnabledTrueOrderByIdAscPathAsc(taskId)
        return permittedFiles
    }

    fun getTaskFilesByType(taskId: Long?, isGrading: Boolean): List<TaskFile> {
        return taskFileRepository.findByTask_IdAndEnabledTrue(taskId)
            .stream().filter { file: TaskFile -> file.grading && isGrading }.toList()
    }

    fun getSubmissions(taskId: Long?, userId: String?): List<Submission> {
        val unrestricted = submissionRepository.findByEvaluation_Task_IdAndUserId(taskId, userId)
        unrestricted.forEach { submission ->
            submission.logs?.let { output ->
                if (submission.command == Command.GRADE) {
                    submission.output = "Logs:\n$output\n\nHint:\n${submission.output}"
                } else {
                    submission.output = output
                }
            }
        }
        val restricted =
            submissionRepository.findByEvaluation_Task_IdAndUserIdAndCommand(taskId, userId, Command.GRADE)
        return Stream.concat(unrestricted.stream(), restricted.stream())
            .sorted(Comparator.comparingLong { obj: Submission -> obj.id!! } // TODO: safety
                .reversed()).toList()
    }

    fun getEvaluation(taskId: Long?, userId: String?): Evaluation? {
        return evaluationRepository.getTopByTask_IdAndUserIdOrderById(taskId, userId)
    }

    fun getRemainingAttempts(taskId: Long?, userId: String?, maxAttempts: Int): Int {
        return getEvaluation(taskId, verifyUserId(userId))?.remainingAttempts ?: maxAttempts
    }

    fun getNextAttemptAt(taskId: Long?, userId: String?): LocalDateTime? {
        return getEvaluation(taskId, verifyUserId(userId))?.nextAttemptAt
    }

    fun getAssignmentDeadlineForTask(taskId: Long?): LocalDateTime? {
        return getTaskById(taskId!!).assignment?.end
    }

    fun createEvent(ordinalNum: Int?, date: LocalDateTime?, type: String?): Event {
        val newEvent = Event()
        newEvent.date = date
        newEvent.type = type
        newEvent.description = "Assignment $ordinalNum is $type."
        return newEvent
    }

    fun getEvents(courseSlug: String?): List<Event> {
        return getAssignments(courseSlug).stream().flatMap<Event> { assignment: AssignmentWorkspace ->
            Stream.of<Event>(
                createEvent(assignment.ordinalNum, assignment.start, "published"),
                createEvent(assignment.ordinalNum, assignment.end, "due")
            )
        }.toList()
    }

    @Cacheable(value = ["calculateAvgTaskPoints"], key = "#taskSlug")
    fun calculateAvgTaskPoints(taskSlug: String?): Double {
        return 0.0
        //return evaluationRepository.findByTask_SlugAndBestScoreNotNull(taskSlug).map {
        //    it.bestScore!! }.average().takeIf { it.isFinite() } ?: 0.0
    }

    fun calculateTaskPoints(taskId: Long?, userId: String?): Double {
        // TODO!: make sure evaluations are saved under only a single user ID in the future!
        // for now, retrieve all possible user IDs from keycloak and retrieve all matching evaluations
        val userIds = roleService.getAllUserIdsFor(verifyUserId(userId))
        return userIds.maxOfOrNull {
            getEvaluation(taskId, verifyUserId(it))?.bestScore ?: 0.0
        } ?: 0.0
    }

    fun calculateAssignmentPoints(tasks: List<Task>, userId: String?): Double {
        return tasks.stream().mapToDouble { task: Task -> calculateTaskPoints(task.id, userId) }.sum()
    }

    fun calculateAssignmentMaxPoints(tasks: List<Task>, userId: String?): Double {
        return tasks.stream().mapToDouble { it.maxPoints!! }.sum()
    }

    fun calculateCoursePoints(assignments: List<Assignment>, userId: String?): Double {
        return assignments.stream()
            .mapToDouble { assignment: Assignment -> calculateAssignmentPoints(assignment.tasks, userId) }
            .sum()
    }

    fun getMaxPoints(courseSlug: String?): Double {
        return getAssignments(courseSlug).sumOf { it.maxPoints!! }
    }

    fun getRank(courseId: Long?): Int {
        val userId = verifyUserId(null)
        return ListUtils.indexOf(getLeaderboard(courseId)) { rank: Rank -> rank.email == userId } + 1
    }

    fun getLeaderboard(courseId: Long?): List<Rank> {
        return evaluationRepository.getCourseRanking(courseId).stream()
            .sorted(Comparator.comparingDouble { obj: Rank -> obj.score }
                .reversed()).toList()
    }

    fun getTeamMembers(memberIds: List<String>): Set<MemberOverview> {
        return memberIds.map { courseRepository.getTeamMemberName(it) }.filterNotNull().toSet()
    }

    fun getTaskBySlug(courseSlug: String, assignmentSlug: String, taskSlug: String): Task {
        return taskRepository.getByAssignment_Course_SlugAndAssignment_SlugAndSlug(
            courseSlug,
            assignmentSlug,
            taskSlug
        ) ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND, "No task found with the URL $taskSlug"
            )
    }

    fun getGradingFiles(taskId: Long?): List<TaskFile> {
        return taskFileRepository.findByTask_IdAndEnabledTrue(taskId)
            .filter { file: TaskFile -> file.grading }
            .toList()
    }

    fun getVisibleNonEditableFiles(taskId: Long?): List<TaskFile> {
        return taskFileRepository.findByTask_IdAndEnabledTrue(taskId)
            .filter { file: TaskFile -> file.visible && !file.editable }
            .toList()
    }

    private fun readLogsFile(path: Path): String {
        val logsFile = path.resolve("logs.txt").toFile()

        return if (!logsFile.exists()) {
            listOf("no log file")
        } else {
            val lines = FileUtils.readLines(logsFile, Charset.defaultCharset())
            when {
                lines.size > 100 -> lines.take(50) + "[... ${lines.size - 100} more lines ...]" + lines.takeLast(50)
                else -> lines
            }
        }.joinToString(separator = "\n").replace("\u0000", "")
    }


    private fun createEvaluation(taskId: Long, userId: String): Evaluation {
        val newEvaluation = getTaskById(taskId).createEvaluation(userId)
        newEvaluation.userId = userId
        return evaluationRepository.save(newEvaluation)
    }

    private fun createSubmissionFile(submission: Submission, fileDTO: SubmissionFileDTO) {
        val newSubmissionFile = SubmissionFile()
        newSubmissionFile.submission = submission
        newSubmissionFile.content = fileDTO.content
        newSubmissionFile.taskFile = getTaskFileById(fileDTO.taskFileId!!)
        submission.files.add(newSubmissionFile)
        submissionRepository.saveAndFlush(submission)
    }

    @Caching(evict = [
        CacheEvict(value = ["getStudent"], key = "#courseSlug + '-' + #submissionDTO.userId"),
        CacheEvict(value = ["getStudentWithPoints"], key = "#courseSlug + '-' + #submissionDTO.userId"),
        CacheEvict(value = ["calculateAvgTaskPoints"], key = "#taskSlug")]
    )
    fun createSubmission(courseSlug: String, assignmentSlug: String, taskSlug: String, submissionDTO: SubmissionDTO) {
        val task = getTaskBySlug(courseSlug, assignmentSlug, taskSlug)
        submissionDTO.command?.let {
            if (!task.hasCommand(it)) throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Submission rejected - no ${submissionDTO.command} command!"
            )
        }
        val evaluation = getEvaluation(task.id, submissionDTO.userId) ?: task.createEvaluation(submissionDTO.userId)
        evaluationRepository.saveAndFlush(evaluation)
        val newSubmission = evaluation.addSubmission(modelMapper.map(submissionDTO, Submission::class.java))
        if (submissionDTO.restricted && newSubmission.isGraded) {
            // TODO: this is just here as documentation, remove when obsolete:
            //if (!task.assignment?.isActive!!) throw ResponseStatusException(
            //    HttpStatus.FORBIDDEN,
            //    "Submission rejected - assignment is not active!"
            //)
            if (evaluation.remainingAttempts!! <= 0) throw ResponseStatusException( // TODO: safety
                HttpStatus.FORBIDDEN,
                "Submission rejected - no remaining attempts!"
            )
        }
        val submission = submissionRepository.saveAndFlush(newSubmission)
        submissionDTO.files.stream().filter { fileDTO -> fileDTO.content != null }
            .forEach { fileDTO: SubmissionFileDTO -> createSubmissionFile(submission, fileDTO) }
        submission.valid = !submission.isGraded
        val course = getCourseBySlug(courseSlug)
        val globalFiles = course.globalFiles
        try {
            task.dockerImage?.let {
                try  {
                    dockerClient.inspectImageCmd(it).exec()
                }catch (e: NotFoundException) {
                    dockerClient.pullImageCmd(it)
                        .exec(PullImageResultCallback())
                        .awaitCompletion()
                }
                dockerClient.createContainerCmd(it).use { containerCmd ->
                    val submissionDir = workingDir.resolve("submissions").resolve(submission.id.toString())
                    // add submission files (supplied by the frontend)
                    submission.files.forEach { file ->
                        createContainerFile(submissionDir, file)
                    }
                    // add visible but not editable files (because these are not part of the submission files)
                    getVisibleNonEditableFiles(task.id)
                        .forEach(Consumer { file: TaskFile ->
                            createContainerFile(submissionDir, file)
                        })
                    // add grading files if submission is graded
                    if (submission.isGraded) {
                        getGradingFiles(task.id)
                            .forEach(Consumer { file: TaskFile ->
                                createContainerFile(submissionDir, file)
                            })
                        globalFiles.forEach { file ->
                            createContainerFile(submissionDir, file)
                        }
                    }
                    // The student code is run on the tmpfs.
                    // Main reason for this is that we have no other way of enforcing a disk quota.
                    // TODO: make this size configurable in task config.toml?
                    val tmpfs: Map<String, String> = mapOf(
                        "/workspace" to "size=50M",
                    )

                    // TODO: make this size configurable in task config.toml?
                    val resultFileSizeLimit = convertSizeToBytes("100K")
                    val persistentFileCopyCommands = task.persistentResultFilePaths.joinToString("\n") { path ->
"""
# Check if results file exceeds permissible size limit
if [[ -f "$path" ]]; then
    actual_size=${'$'}(stat -c%s "$path")
    if [[ ! ${'$'}actual_size -lt $resultFileSizeLimit ]]; then
        exit 202
    fi
fi
file_dir=${'$'}(dirname "$path")
mkdir -p "/submission/${'$'}file_dir"
cp "$path" "/submission/${'$'}file_dir" 
"""
                    }
                    val command = (
"""
# copy submitted files to tmpfs
/bin/cp -R /submission/* /workspace/;
# run command (the cwd is set to /workspace already)
${task.formCommand(submission.command!!)} &> logs.txt;
# remember the command's exit code
exit_code=${'$'}?; 
# write results and logs to submission volume
/bin/cp /workspace/grade_results.json /submission/;
/bin/cp /workspace/logs.txt /submission/;
# check if the tmpfs is full and if so, return 201
USAGE=${'$'}(df -h | grep /workspace | awk '{print ${'$'}5}' | sed 's/%//')
if [ "${'$'}USAGE" -eq 100 ]; then
    exit 201
fi
# otherwise check and copy persistent results and return command status code
$persistentFileCopyCommands
exit ${'$'}exit_code; 
"""
                    )
                    val container = containerCmd
                        .withLabels(mapOf("userId" to submission.userId)).withWorkingDir("/workspace")
                        .withCmd("/bin/bash", "-c", command)
                        .withHostConfig(
                            HostConfig()
                                .withTmpFs(tmpfs)
                                .withMemory(536870912L)
                                .withPrivileged(true)
                                .withBinds(Bind.parse("$submissionDir:/submission"))
                                .withAutoRemove(true)
                        ).exec()

                    val scheduler = Executors.newScheduledThreadPool(1)
                    var killedContainer = false
                    scheduler.schedule({
                        try {
                            dockerClient.stopContainerCmd(container.id).withTimeout(0).exec()
                            killedContainer = true
                            logger.debug { "Stopped container ${container.id}"}
                        } catch (e: Exception) {
                            logger.debug { "Container ${container.id} probably stopped already"}
                        }
                    }, task.timeLimit.coerceAtMost(180).toLong(), TimeUnit.SECONDS)
                    dockerClient.startContainerCmd(container.id).exec()

                    val statusCode = dockerClient.waitContainerCmd(container.id)
                        .exec(WaitContainerResultCallback())
                        .awaitStatusCode()

                    scheduler.shutdown()

                    submission.logs = readLogsFile(submissionDir)
                    logger.debug { "Submission $submissionDir finished with statusCode $statusCode"}
                    val persistentResultFileErrors: MutableList<String> = mutableListOf()
                    task.persistentResultFilePaths.forEach { path ->
                        try {
                            val resultFile = fileService.storeFile(submissionDir.resolve(path), ResultFile())
                            resultFile.path = path
                            resultFile.submission = newSubmission
                            newSubmission.persistentResultFiles.add(resultFile)
                        } catch (e: Exception) { // TODO: are there other specific exceptions to catch?
                            persistentResultFileErrors.add("A file '$path' should have been created, but wasn't.")
                        }
                    }
                    if (newSubmission.isGraded) {
                        val results = when (statusCode) {
                            // out of memory
                            137 -> {
                                logger.debug { "Submission $submissionDir exit code is 137" }
                                if (killedContainer) {
                                    logger.debug { "Submission $submissionDir killed due to timeout" }
                                    Results(
                                        0.0,
                                        mutableListOf("Your solution ran out of time. Check for infinite loops and ensure your solution is sufficiently fast even for challenging problem parameters.")
                                    )
                                } else {
                                    logger.debug { "Submission $submissionDir out of memory" }
                                    Results(
                                        0.0,
                                        mutableListOf("Your solution ran out of memory. Make sure you aren't creating gigantic data structures.")
                                    )
                                }
                            }
                            // out of tmpfs disk space
                            201 -> {
                                Results(
                                    0.0,
                                    mutableListOf("Your solution wrote too much data, either to files, or by printing to the command line. Are you printing in an infinite loop?")
                                )
                            }
                            // persistent result file too large
                            202 -> {
                                Results(
                                    0.0,
                                    mutableListOf("One or more files you're supposed to write exceeds the file size limit of ${bytesToHumanReadable(resultFileSizeLimit)}")
                                )
                            }
                            // none of the above, hopefully there are grading results
                            else -> {
                                try {
                                    jsonMapper.readValue(Files.readString(submissionDir.resolve("grade_results.json")), Results::class.java)
                                } catch (e: NoSuchFileException) {
                                    logger.debug { "Submission $submissionDir no grade_results.json" }
                                    Results(null,
                                        mutableListOf("No grading results. Please report this as a bug and provide as much detail as possible.")
                                    )
                                }
                            }
                        }
                        results.hints.addAll(persistentResultFileErrors)
                        newSubmission.output = results.hints.filterNotNull().firstOrNull()
                        if (results.points != null) {
                            newSubmission.valid = true
                            // never go over 100%; the number of points is otherwise up to the test suite to determine correctly
                            newSubmission.points = minOf(results.points!!, newSubmission.maxPoints!!)
                            evaluation.update(newSubmission.points)
                        }
                    }
                    // TODO: move to finally? For the moment, we keep failed submission dirs around for debugging.
                    FileUtils.deleteQuietly(submissionDir.toFile())
                }
            }
        } catch (e: Exception) {
            newSubmission.output = "Uncaught ${e::class.simpleName}: ${e.message}. Please report this as a bug and provide as much detail as possible."
        }
        submissionRepository.save(newSubmission)
    }

    private fun createContainerFile(submissionDir: Path, relativePath: String): Path {
        val unrootedFilePath = relativePath.substring(1)
        val filePath = submissionDir.resolve(unrootedFilePath)
        Files.createDirectories(filePath.parent)
        if (!filePath.toFile().exists()) Files.createFile(filePath)
        return filePath
    }

    private fun createContainerFile(submissionDir: Path, submissionFile: SubmissionFile) {
        val filePath = createContainerFile(submissionDir, submissionFile.taskFile!!.path!!)
        Files.writeString(filePath, submissionFile.content)
    }

    private fun writeContainerFile(filePath: Path, data: String?, binaryData: ByteArray?) {
        if (binaryData != null) { Files.write(filePath, binaryData) }
        else { Files.writeString(filePath, data) }
    }

    private fun createContainerFile(submissionDir: Path, taskFile: TaskFile) {
        val filePath = createContainerFile(submissionDir, taskFile.path!!)
        writeContainerFile(filePath, taskFile.template, taskFile.templateBinary)
    }

    private fun createContainerFile(submissionDir: Path, globalFile: GlobalFile) {
        val filePath = createContainerFile(submissionDir, globalFile.path!!)
        writeContainerFile(filePath, globalFile.template, globalFile.templateBinary)
    }

    private fun createLocalFile(submissionDir: Path, relativeFilePath: String, content: String) {
        val unrootedFilePath = relativeFilePath.substring(1)
        val filePath = submissionDir.resolve(unrootedFilePath)
        Files.createDirectories(filePath.parent)
        if (!filePath.toFile().exists()) Files.createFile(filePath)
        Files.writeString(filePath, content)
    }

    fun createCourse(course: CourseDTO): Course {
        return courseLifecycle.createFromRepository(course)
    }

    @Transactional
    fun editCourse(course: CourseDTO): Course {
        val existingCourse = getCourseBySlug(course.slug!!)
        existingCourse.repository = course.repository
        existingCourse.repositoryUser = course.repositoryUser
        existingCourse.repositoryPassword = course.repositoryPassword
        existingCourse.webhookSecret = course.webhookSecret
        courseRepository.save(existingCourse)
        return courseLifecycle.updateFromRepository(existingCourse)
    }

    @Transactional
    fun webhookUpdateWithSecret(courseSlug: String, secret: String?): Course? {
        val existingCourse = getCourseBySlug(courseSlug)
        if (existingCourse.webhookSecret != null && secret != null) {
            if (existingCourse.webhookSecret == secret) {
                return updateCourse(courseSlug)
            }
        }
        logger.debug { "Provided webhook secret does not match secret of course $courseSlug"}
        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }

    fun webhookUpdateWithHmac(courseSlug: String, signature: String?, body: String): Course? {
        val existingCourse = getCourseBySlug(courseSlug)
        if (existingCourse.webhookSecret != null && signature != null) {
            val key = SecretKeySpec(existingCourse.webhookSecret!!.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(key)
            val hmac = mac.doFinal(body.toByteArray(Charsets.UTF_8))
            val expected = DatatypeConverter.printHexBinary(hmac)
            if (expected.equals(signature, ignoreCase = true)) {
                return updateCourse(courseSlug)
            }
        }
        logger.debug { "Provided webhook signature does not match secret of course $courseSlug"}
        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }

    @Transactional
    fun updateCourse(courseSlug: String): Course {
        val existingCourse = getCourseBySlug(courseSlug)
        return courseLifecycle.updateFromRepository(existingCourse)
    }

    @Transactional
    fun updateCourseFromDirectory(courseSlug: String, directory: Path): Course {
        val existingCourse = getCourseBySlug(courseSlug)
        return courseLifecycle.updateFromDirectory(existingCourse, directory )
    }

    @Transactional
    fun deleteCourse(courseSlug: String): Course {
        val existingCourse = getCourseBySlug(courseSlug)
        return courseLifecycle.delete(existingCourse)
    }

    fun sendMessage(contactDTO: ContactDTO) {
        val filePath = workingDir.resolve("contact").resolve(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        Files.createDirectories(filePath.parent)
        if (!filePath.toFile().exists()) Files.createFile(filePath)
        Files.writeString(filePath, contactDTO.formatContent())
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["getUserByUsername"], key = "#username"),
        CacheEvict(value = ["getUserRoles"], key = "#username")
    ])
    fun updateStudentRoles(username: String) {
        getCourses().forEach { course ->
            logger.debug { "syncing to ${course.slug}"}
            roleService.updateStudentRoles(course, course.registeredStudents, username)
        }
    }

    @Cacheable(value = ["getStudent"], key = "#courseSlug + '-' + #user.username")
    fun getStudent(courseSlug: String, user: UserRepresentation): StudentDTO {
        return StudentDTO(user.firstName, user.lastName, user.email)
    }

    @Cacheable(value = ["getStudentWithPoints"], key = "#courseSlug + '-' + #user.username")
    fun getStudentWithPoints(courseSlug: String, user: UserRepresentation): StudentDTO {
        val coursePoints = calculateCoursePoints(getCourseBySlug(courseSlug).assignments, user.username)
        return StudentDTO(user.firstName, user.lastName, user.email, coursePoints)
    }

    private fun getEvaluation(task: Task, userId: String): EvaluationSummary? {
        return evaluationRepository.findTopByTask_IdAndUserIdOrderById(task.id, userId)
    }

    fun getTaskProgress( courseSlug: String, assignmentSlug: String, taskSlug: String, userId: String): TaskProgressDTO {
        val task = getTaskBySlug(courseSlug, assignmentSlug, taskSlug)
        val evaluation = getEvaluation(task, userId)
        if (evaluation == null) {
            return TaskProgressDTO(
                userId,
                taskSlug,
                0.0,
                task.maxPoints,
                task.maxAttempts,
                task.maxAttempts,
                task.information.map { (language, info) -> language to TaskInformationDTO(info.language, info.title, info.instructionsFile) }.toMap().toMutableMap(),
                listOf()
            )
        } else {
            return TaskProgressDTO(
                userId,
                taskSlug,
                evaluation.bestScore,
                task.maxPoints,
                evaluation.remainingAttempts,
                task.maxAttempts,
                task.information.map { (language, info) -> language to TaskInformationDTO(info.language, info.title, info.instructionsFile) }.toMap().toMutableMap(),
                listOf()
            )
        }
    }

    private fun getTasksProgress(assignment: Assignment, userId: String): List<TaskProgressDTO> {
        return assignment.tasks.mapNotNull { task ->
            getTaskProgress(
                assignment.course!!.slug!!,
                assignment.slug!!,
                task.slug!!,
                userId
            )
        }
    }

    fun getAssignmentProgress(courseSlug: String, assignmentSlug: String, userId: String): AssignmentProgressDTO {
        val assignment: Assignment = getAssignmentBySlug(courseSlug, assignmentSlug)
        return AssignmentProgressDTO(userId, assignmentSlug,
            assignment.information.map { (language, info) -> language to AssignmentInformationDTO(info.language, info.title) }.toMap().toMutableMap(),
            getTasksProgress(assignment, userId))
    }

    fun getCourseProgress(courseSlug: String, userId: String): CourseProgressDTO {
        val course: Course = getCourseBySlug(courseSlug)
        return CourseProgressDTO(userId,
            course.information.map { (language, info) -> language to CourseInformationDTO(
                info.language, info.title, info.description, info.university, info.period) }.toMap().toMutableMap(),
            course.assignments.filter{ it.isPublished }. map { assignment ->
                AssignmentProgressDTO(
                    null,
                    assignment.slug!!,
                    assignment.information.map { (language, info) -> language to AssignmentInformationDTO(info.language, info.title) }.toMap().toMutableMap(),
                    getTasksProgress(assignment, userId)
                )
            }.toList())
    }

    fun registerStudents(courseSlug: String, students: List<String>) {
        val course: Course = getCourseBySlug(courseSlug)
        println(students)
        course.registeredStudents = students.toMutableSet()
        courseRepository.save(course)
        logger.debug { "Registered ${course.registeredStudents.size} in course $courseSlug"}
        println()
    }

    fun convertSizeToBytes(sizeStr: String): Long {
        val regex = Regex("""^(\d+)([KMG]?)$""")
        val matchResult = regex.matchEntire(sizeStr)

        return matchResult?.let {
            val (number, suffix) = it.destructured
            val sizeInBytes = when (suffix) {
                "K" -> number.toLong() * 1024
                "M" -> number.toLong() * 1024 * 1024
                "G" -> number.toLong() * 1024 * 1024 * 1024
                "" -> number.toLong()
                else -> 0
            }
            sizeInBytes
        } ?: 0
    }

    fun bytesToHumanReadable(sizeInBytes: Long): String {
        val kilobyte = 1024.0
        val megabyte = kilobyte * 1024
        val gigabyte = megabyte * 1024

        return when {
            sizeInBytes < kilobyte -> "$sizeInBytes B"
            sizeInBytes < megabyte -> String.format("%.2f KB", sizeInBytes / kilobyte)
            sizeInBytes < gigabyte -> String.format("%.2f MB", sizeInBytes / megabyte)
            else -> String.format("%.2f GB", sizeInBytes / gigabyte)
        }
    }
}