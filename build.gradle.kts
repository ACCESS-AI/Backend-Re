import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("jacoco")
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.3"
    id("org.sonarqube") version "4.3.0.3225"
    id("io.freefair.lombok") version "8.2.2"
    id("com.github.ben-manes.versions") version "0.47.0"
    id("org.flywaydb.flyway") version "9.21.1"
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"
    kotlin("plugin.jpa") version "1.9.0"
    kotlin("plugin.allopen") version "1.9.0"

}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("lombok.Data")
}


group = "ch.uzh.ifi"
version = "0.0.2"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        // Chatbot library
        url = uri("../Chatbot/build/repo")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.security:spring-security-test")
    implementation("org.springframework.security:spring-security-data:6.1.3")
    implementation("org.springframework.security:spring-security-oauth2-jose:6.1.3")
    implementation("org.springdoc:springdoc-openapi-ui:1.7.0")
    implementation("org.springdoc:springdoc-openapi-security:1.7.0")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.modelmapper:modelmapper:3.1.1")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.keycloak:keycloak-admin-client:22.0.1")
    implementation("com.github.docker-java:docker-java:3.3.3")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.3")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.apache.httpcomponents:httpcore:4.4.16")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
    implementation("org.apache.tika:tika-core:2.8.0")
    implementation("org.flywaydb:flyway-core")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.mockito", "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.junit.vintage", "junit-vintage-engine")
        exclude("junit", "junit")
    }
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")

    // Chatbot
    implementation("ch.uzh.ifi:access-chatbot:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.5.2")
    implementation("org.jboss:jandex:3.1.6")
    implementation("com.google.guava:guava:33.2.1-jre")

}
/*
test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}


jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
    }
}

sonarqube {
    properties {
        property "sonar.projectKey", "mp-access_Backend"
        property "sonar.organization", "access"
        property "sonar.host.url", "https://sonarcloud.io"
    }
}
*/

/**
 * Build and push a Docker image of the backend to DockerHub via the built-in gradle task.
 * Requires passing 2 environment variables:
 * (1) username     - username to utilize for logging into DockerHub
 * (2) password     - password to utilize for logging into DockerHub
 * These are already defined in the deployment environment (as GitHub Actions secrets).
 * For usage see the deployment script under .github/workflows/backend.yml.
 */

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    if (project.hasProperty("username")) {
        imageName.set("sealuzh/access-backend:x")
        publish.set(true)
        docker.publishRegistry.username.set(project.property("username") as String)
        docker.publishRegistry.password.set(project.property("password") as String)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    filter {
        includeTestsMatching("ch.uzh.ifi.access.AllTests")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

flyway {
    url = "jdbc:postgresql://localhost:5432/access"
    user = "admin"
    password = "admin"
}
