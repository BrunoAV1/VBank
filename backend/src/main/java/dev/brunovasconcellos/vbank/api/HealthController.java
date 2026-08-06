package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties properties;
    public HealthController(JdbcTemplate jdbcTemplate, AppProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @GetMapping
    ResponseEntity<ApiDtos.HealthResponse> health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return ResponseEntity.ok(new ApiDtos.HealthResponse("UP", "VBank Sandbox", "CONNECTED", properties.getVersion(), Instant.now()));
            }
        } catch (RuntimeException ignored) {
            // A resposta deliberadamente não inclui host, credencial ou causa interna.
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiDtos.HealthResponse("DOWN", "VBank Sandbox", "UNAVAILABLE", properties.getVersion(), Instant.now()));
    }
}
