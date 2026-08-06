package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.domain.AuditLog;
import dev.brunovasconcellos.vbank.domain.User;
import dev.brunovasconcellos.vbank.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(User user, String action, String outcome, String targetType, String targetId, String metadata) {
        String actor = user == null ? "não autenticado" : user.getUsername();
        repository.save(new AuditLog(user, action, outcome, actor, targetType, targetId, sanitize(metadata)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(User user, String action, String actorLabel, String metadata) {
        repository.save(new AuditLog(user, action, "DENIED", sanitize(actorLabel), null, null, sanitize(metadata)));
    }

    private String sanitize(String value) {
        if (value == null) return null;
        String clean = value.replaceAll("(?i)(password|senha|pin|token|secret|cookie|authorization)\\s*[=:]\\s*[^,; ]+", "$1=[REDACTED]");
        return clean.substring(0, Math.min(clean.length(), 500));
    }
}
