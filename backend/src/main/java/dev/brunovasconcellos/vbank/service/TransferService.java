package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class TransferService {
    private final TransferExecutor executor;
    private final PinService pinService;
    private final RateLimitService rateLimitService;
    private final AuditService auditService;

    public TransferService(TransferExecutor executor, PinService pinService,
                           RateLimitService rateLimitService, AuditService auditService) {
        this.executor = executor;
        this.pinService = pinService;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
    }

    public ApiDtos.TransferResponse transfer(UUID userId, ApiDtos.TransferRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        ApiDtos.TransferResponse replay = executor.findExisting(userId, idempotencyKey, request);
        if (replay != null) return replay;
        rateLimitService.consume("transfer", userId.toString(), 20, Duration.ofMinutes(1));
        pinService.verifyForTransfer(userId, request.pin());
        try {
            return executor.execute(userId, request, idempotencyKey);
        } catch (ApiException exception) {
            auditService.recordFailure(null, "TRANSFER_DENIED", userId.toString(), "code=" + exception.getCode());
            throw exception;
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !key.matches("^[A-Za-z0-9._:-]{8,100}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Envie um cabeçalho Idempotency-Key válido com 8 a 100 caracteres.");
        }
    }
}
