package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Account;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.PixKey;
import dev.brunovasconcellos.vbank.repository.AccountRepository;
import dev.brunovasconcellos.vbank.repository.PixKeyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PixKeyService {
    private final PixKeyRepository pixKeyRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final RateLimitService rateLimitService;

    public PixKeyService(PixKeyRepository pixKeyRepository, AccountRepository accountRepository,
                         AuditService auditService, RateLimitService rateLimitService) {
        this.pixKeyRepository = pixKeyRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.PixKeyResponse> list(UUID userId) {
        Account account = requireAccount(userId);
        return pixKeyRepository.findByAccountAndStatus(account.getId(), Enums.PixKeyStatus.ACTIVE)
                .stream().map(DtoMapper::pix).toList();
    }

    @Transactional
    public ApiDtos.PixKeyResponse create(UUID userId, ApiDtos.PixKeyRequest request) {
        Account account = requireAccount(userId);
        if (account.getStatus() != Enums.AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_BLOCKED", "A conta não pode criar chaves internas.");
        }
        String display;
        String normalized;
        if (request.type() == Enums.PixKeyType.RANDOM) {
            display = UUID.randomUUID().toString();
            normalized = display;
        } else {
            if (request.value() == null || request.value().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Informe o valor da chave interna.");
            }
            if (DocumentDetector.looksLikeCpfOrCnpj(request.value())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "CPF e CNPJ não são aceitos neste ambiente.");
            }
            normalized = DomainNormalizer.pixKey(request.type(), request.value());
            display = normalized;
            if (request.type() == Enums.PixKeyType.EMAIL && !normalized.equals(account.getUser().getEmail())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "A chave de e-mail deve ser o e-mail da conta de demonstração.");
            }
            if (request.type() == Enums.PixKeyType.USERNAME && !normalized.equals("@" + account.getUser().getUsername())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "A chave de username deve usar o username da conta.");
            }
        }
        try {
            PixKey key = pixKeyRepository.saveAndFlush(new PixKey(account, request.type(), display, normalized));
            auditService.record(account.getUser(), "PIX_KEY_CREATED", "SUCCESS", "PIX_KEY", key.getId().toString(), "tipo=" + request.type());
            return DtoMapper.pix(key);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "PIX_KEY_ALREADY_EXISTS", "Esta chave interna já está ativa em outra conta.");
        }
    }

    @Transactional
    public void delete(UUID userId, UUID keyId) {
        Account account = requireAccount(userId);
        PixKey key = pixKeyRepository.findById(keyId)
                .filter(item -> item.getAccount().getId().equals(account.getId()))
                .filter(item -> item.getStatus() == Enums.PixKeyStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PIX_KEY_NOT_FOUND", "Chave interna não encontrada."));
        key.delete();
        auditService.record(account.getUser(), "PIX_KEY_DELETED", "SUCCESS", "PIX_KEY", keyId.toString(), null);
    }

    public ApiDtos.ResolvedPixKeyResponse resolve(UUID userId, String value) {
        rateLimitService.consume("pix-resolve", userId.toString(), 40, Duration.ofMinutes(1));
        return resolveReadOnly(value);
    }

    @Transactional(readOnly = true)
    public ApiDtos.ResolvedPixKeyResponse resolveReadOnly(String value) {
        PixKey key = requireActive(value);
        Account account = key.getAccount();
        if (account.getStatus() == Enums.AccountStatus.SYSTEM) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PIX_KEY_NOT_FOUND", "Chave interna não encontrada.");
        }
        return new ApiDtos.ResolvedPixKeyResponse(DtoMapper.maskName(account.getUser().getFullName()), key.getType(),
                key.getDisplayValue(), account.getStatus().name(), true);
    }

    @Transactional(readOnly = true)
    public PixKey requireActive(String value) {
        String normalized = DomainNormalizer.resolvePixKey(value);
        return pixKeyRepository.findByNormalizedValueAndStatus(normalized, Enums.PixKeyStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PIX_KEY_NOT_FOUND", "Nenhuma conta de demonstração usa esta chave interna."));
    }

    private Account requireAccount(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Conta de demonstração não encontrada."));
    }
}
