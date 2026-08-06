package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.User;
import dev.brunovasconcellos.vbank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PinServiceTest {
    private User user;
    private PinService service;

    @BeforeEach
    void setUp() {
        UserRepository repository = mock(UserRepository.class);
        AuditService audit = mock(AuditService.class);
        RateLimitService rateLimit = mock(RateLimitService.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        user = new User("Pessoa Teste", "pessoa@example.test", "pessoa", encoder.encode("Senha123"), Set.of(Enums.Role.USER));
        user.setPinHash(encoder.encode("123456"));
        when(repository.findLockedById(user.getId())).thenReturn(Optional.of(user));
        service = new PinService(repository, encoder, audit, rateLimit);
    }

    @Test
    void resetsAttemptsAfterCorrectPin() {
        user.setPinFailedAttempts(2);
        service.verifyForTransfer(user.getId(), "123456");
        assertThat(user.getPinFailedAttempts()).isZero();
        assertThat(user.getPinBlockedUntil()).isNull();
    }

    @Test
    void blocksAfterFiveWrongAttempts() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> service.verifyForTransfer(user.getId(), "000000"))
                    .isInstanceOf(ApiException.class);
        }
        assertThat(user.getPinFailedAttempts()).isEqualTo(5);
        assertThat(user.getPinBlockedUntil()).isNotNull();
        assertThatThrownBy(() -> service.verifyForTransfer(user.getId(), "123456"))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo("PIN_TEMPORARILY_BLOCKED"));
    }
}
