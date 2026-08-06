package dev.brunovasconcellos.vbank.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException exception, HttpServletRequest request) {
        return problem(exception.getStatus(), exception.getCode(), exception.getMessage(), request, null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ProblemDetail> handleValidation(Exception exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (exception instanceof MethodArgumentNotValidException invalid) {
            invalid.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        }
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Revise os campos informados.", request, errors);
    }

    @ExceptionHandler({CannotAcquireLockException.class})
    ResponseEntity<ProblemDetail> handleLock(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "Operação concorrente em andamento. Tente novamente.", request, null);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetail> handleDatabase(DataAccessException exception, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE", "O banco de dados está temporariamente indisponível.", request, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "SERVICE_UNAVAILABLE", "Não foi possível concluir a operação.", request, null);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail,
                                                   HttpServletRequest request, Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://vbank.local/problems/" + code.toLowerCase().replace('_', '-')));
        problem.setTitle(title(code));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("traceId", MDC.get("traceId") == null ? request.getAttribute("traceId") : MDC.get("traceId"));
        if (errors != null && !errors.isEmpty()) problem.setProperty("errors", errors);
        return ResponseEntity.status(status).body(problem);
    }

    private String title(String code) {
        return switch (code) {
            case "INVALID_CREDENTIALS" -> "Credenciais inválidas";
            case "ACCOUNT_BLOCKED" -> "Conta bloqueada";
            case "INVALID_PIN" -> "PIN inválido";
            case "PIN_TEMPORARILY_BLOCKED" -> "PIN temporariamente bloqueado";
            case "PIX_KEY_NOT_FOUND" -> "Chave interna não encontrada";
            case "PIX_KEY_ALREADY_EXISTS" -> "Chave interna já cadastrada";
            case "INSUFFICIENT_BALANCE" -> "Saldo insuficiente";
            case "DAILY_LIMIT_EXCEEDED" -> "Limite diário excedido";
            case "SELF_TRANSFER_NOT_ALLOWED" -> "Transferência para a própria conta";
            case "DUPLICATE_IDEMPOTENCY_KEY" -> "Chave de idempotência já utilizada";
            case "SANDBOX_FUNDING_NOT_AVAILABLE" -> "Recarga indisponível";
            case "VALIDATION_ERROR" -> "Dados inválidos";
            case "DATABASE_UNAVAILABLE" -> "Banco indisponível";
            default -> "Serviço indisponível";
        };
    }
}
