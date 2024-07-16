package ch.uzh.ifi.access

import ch.uzh.ifi.access.service.CourseLifecycleTests
import ch.uzh.ifi.access.service.PublicAPITests
import org.junit.jupiter.api.ClassOrderer
import org.junit.jupiter.api.TestClassOrder
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@AutoConfigureMockMvc
@SpringBootTest
abstract class BaseTest

@Suite
@SelectClasses(CourseLifecycleTests::class, PublicAPITests::class)
@TestClassOrder(ClassOrderer.OrderAnnotation::class)
class AllTests