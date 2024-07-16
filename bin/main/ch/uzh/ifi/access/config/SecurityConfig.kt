package ch.uzh.ifi.access.config

import ch.uzh.ifi.access.service.CourseService
import ch.uzh.ifi.access.service.RoleService
import io.github.oshai.kotlinlogging.KotlinLogging
import lombok.AllArgsConstructor
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.RealmResource
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.authorization.AuthorityAuthorizationDecision
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.core.GrantedAuthorityDefaults
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.session.HttpSessionEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.filter.CommonsRequestLoggingFilter
import java.nio.file.Path


@AllArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(private val env: Environment) {

    private val logger = KotlinLogging.logger {}

    private fun parseAuthorities(authorities: List<String>): Collection<GrantedAuthority> {
        return authorities.map { role: String? -> SimpleGrantedAuthority(role) }
    }


    private fun parseAuthorities(token: Jwt): Collection<GrantedAuthority> {
        return token.getClaimAsStringList("enrollments").map { role -> SimpleGrantedAuthority(role)}
    }

    private fun isAuthorizedAPIKey(context: RequestAuthorizationContext): Boolean {
        val apiKey = env.getProperty("API_KEY") ?: return false
        val headerKey = context.request.getHeader("X-API-Key")?.lines()?.joinToString("")
        if (headerKey == null) {
            logger.debug { "${context.request.requestURI}: Missing X-API-Key" }
        }
        return apiKey == headerKey
    }


    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.sessionManagement {
            it.sessionConcurrency{
                it.maximumSessions(1)
            }
        }
        .csrf { it.ignoringRequestMatchers(
            "/courses/contact/**",
            "/courses/{course}/summary",
            "/courses/{course}/participants/**",
            "/webhooks/**")
        }
        .authorizeHttpRequests { authorize ->
            authorize
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/courses/contact/**",
                    "/webhooks/**"
                ).permitAll()
                .requestMatchers(
                    "/courses/{course}/participants/**",
                    "/courses/{course}/summary"
                ).access { _, context ->
                    AuthorityAuthorizationDecision(isAuthorizedAPIKey(context), parseAuthorities(listOf("supervisor")))
                }

            .anyRequest().authenticated()
        }
        .oauth2ResourceServer {
            it.jwt {
                it.jwtAuthenticationConverter { source: Jwt ->
                    JwtAuthenticationToken(
                        source,
                        parseAuthorities(source),
                        source.getClaimAsString("email")
                    )
                }
            }
        }
        return http.build()
    }

    @Bean
    fun grantedAuthorityDefaults(): GrantedAuthorityDefaults {
        return GrantedAuthorityDefaults("")
    }

    @Bean
    fun securityEvaluationContextExtension(): SecurityEvaluationContextExtension {
        return SecurityEvaluationContextExtension()
    }

    @Bean
    fun logFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(400)
        filter.setIncludeHeaders(false)
        return filter
    }

    @Bean
    fun workingDir(): Path {
        return Path.of(env.getProperty("WORKING_DIR", "/workspace/data"))
    }

    @Bean
    fun accessRealm(): RealmResource {
        val keycloakClient = Keycloak.getInstance(
            env.getProperty("AUTH_SERVER_URL", "http://localhost:8080"), "master",
            env.getProperty("KEYCLOAK_ADMIN", "admin"),
            env.getProperty("KEYCLOAK_ADMIN_PASSWORD", "admin"),
            "admin-cli"
        )
        return keycloakClient.realm("access")
    }
}

@Component
class AuthenticationSuccessListener(
    val courseService: CourseService,
    val roleService: RoleService
) : ApplicationListener<AuthenticationSuccessEvent> {

    private val logger = KotlinLogging.logger {}

    override fun onApplicationEvent(event: AuthenticationSuccessEvent) {
        val username = event.authentication.name
        roleService.getUserRepresentationForUsername(username)?.let { user ->
            if (user.attributes?.containsKey("roles_synced_at") != true) {
                logger.debug { "syncing $username to courses" }
                courseService.updateStudentRoles(username)
            }
        }
    }
}
