package ch.uzh.ifi.access

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@RestController
class ServerInfoController {
    @GetMapping("/info")
    fun getAppInfo(@Value("\${access.version}") version: String): Map<String, String> {
        return java.util.Map.of(
            "version", version,
            "offsetDateTime", ZonedDateTime.now().toOffsetDateTime().toString(),
            "utcTime", Instant.now().toString(), "zoneId", ZoneId.systemDefault().toString()
        )
    }
}
