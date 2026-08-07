package dev.brunovasconcellos.vbank.integration;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.repository.AccountRepository;
import dev.brunovasconcellos.vbank.repository.TransferRepository;
import dev.brunovasconcellos.vbank.repository.UserRepository;
import dev.brunovasconcellos.vbank.service.AdminService;
import dev.brunovasconcellos.vbank.service.AccountService;
import dev.brunovasconcellos.vbank.service.AuthService;
import dev.brunovasconcellos.vbank.service.FundingService;
import dev.brunovasconcellos.vbank.service.PinService;
import dev.brunovasconcellos.vbank.service.PixKeyService;
import dev.brunovasconcellos.vbank.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class BankFlowIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("vbank_test").withUsername("vbank").withPassword("vbank");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "integration-test-secret-with-at-least-32-bytes-123456");
        registry.add("app.admin-bootstrap.enabled", () -> "true");
        registry.add("app.admin-bootstrap.name", () -> "Administrador Teste");
        registry.add("app.admin-bootstrap.email", () -> "admin@example.test");
        registry.add("app.admin-bootstrap.username", () -> "admin");
        registry.add("app.admin-bootstrap.password", () -> "AdminSenha123");
        registry.add("app.admin-bootstrap.pin", () -> "654321");
    }

    @Autowired AuthService authService;
    @Autowired PinService pinService;
    @Autowired PixKeyService pixKeyService;
    @Autowired TransferService transferService;
    @Autowired FundingService fundingService;
    @Autowired AdminService adminService;
    @Autowired AccountService accountService;
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransferRepository transferRepository;
    @Autowired RollbackProbe rollbackProbe;

    @Test
    void completeFlowIdempotencyConcurrencyRollbackAndAdministration() throws Exception {
        var aliceSession = register("Alice Sandbox", "alice@example.test", "alice");
        var brunoSession = register("Bruno Sandbox", "bruno@example.test", "bruno");
        UUID aliceId = aliceSession.response().user().id();
        UUID brunoId = brunoSession.response().user().id();
        pinService.create(aliceId, "123456");
        pinService.create(brunoId, "123456");

        assertThat(accountRepository.findByUserId(aliceId).orElseThrow().getBalance()).isEqualByComparingTo("50000.00");
        assertThatThrownBy(() -> register("Alice Duplicada", "alice@example.test", "alice2"))
                .isInstanceOf(ApiException.class);

        pixKeyService.create(brunoId, new ApiDtos.PixKeyRequest(Enums.PixKeyType.EMAIL, "bruno@example.test"));
        assertThat(pixKeyService.resolve(aliceId, "bruno@example.test").maskedName()).startsWith("B");
        assertThatThrownBy(() -> pixKeyService.create(aliceId,
                new ApiDtos.PixKeyRequest(Enums.PixKeyType.PHONE, "bruno@example.test"))).isInstanceOf(ApiException.class);

        ApiDtos.TransferRequest transfer = new ApiDtos.TransferRequest("bruno@example.test", new BigDecimal("1000.00"), "Teste", "123456");
        var first = transferService.transfer(aliceId, transfer, "integration-key-0001");
        var replay = transferService.transfer(aliceId, transfer, "integration-key-0001");
        assertThat(replay.id()).isEqualTo(first.id());
        assertThatThrownBy(() -> transferService.transfer(aliceId,
                new ApiDtos.TransferRequest("bruno@example.test", new BigDecimal("1001.00"), "Teste", "123456"),
                "integration-key-0001"))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo("DUPLICATE_IDEMPOTENCY_KEY"));
        assertThat(accountRepository.findByUserId(aliceId).orElseThrow().getBalance()).isEqualByComparingTo("49000.00");
        assertThat(accountRepository.findByUserId(brunoId).orElseThrow().getBalance()).isEqualByComparingTo("51000.00");

        assertThat(accountService.dashboard(aliceId).recentEntries()).isNotEmpty();
        assertThat(accountService.statement(aliceId, null, null, null, null, null, null, Pageable.unpaged())).isNotEmpty();

        pixKeyService.create(aliceId, new ApiDtos.PixKeyRequest(Enums.PixKeyType.USERNAME, "@alice"));
        assertThatThrownBy(() -> transferService.transfer(aliceId,
                new ApiDtos.TransferRequest("@alice", BigDecimal.ONE, null, "123456"), "self-key-00000001"))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getCode()).isEqualTo("SELF_TRANSFER_NOT_ALLOWED"));
        assertThatThrownBy(() -> transferService.transfer(aliceId,
                new ApiDtos.TransferRequest("bruno@example.test", new BigDecimal("90000"), null, "123456"), "insufficient-0001"))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getCode()).isEqualTo("INSUFFICIENT_BALANCE"));

        var funding = fundingService.fund(aliceId);
        assertThat(funding.amount()).isEqualByComparingTo("1000.00");
        assertThatThrownBy(() -> fundingService.fund(aliceId)).isInstanceOf(ApiException.class);

        var login = authService.login(new ApiDtos.LoginRequest("alice", "SenhaForte123"), "127.0.0.1", "JUnit");
        var rotated = authService.refresh(login.refreshToken(), "JUnit refresh");
        authService.logout(rotated.refreshToken());
        assertThatThrownBy(() -> authService.refresh(rotated.refreshToken(), "JUnit"))
                .isInstanceOf(ApiException.class);

        var rollbackAccount = accountRepository.findByUserId(brunoId).orElseThrow();
        BigDecimal beforeRollback = rollbackAccount.getBalance();
        assertThatThrownBy(() -> rollbackProbe.changeThenFail(rollbackAccount.getId())).isInstanceOf(IllegalStateException.class);
        assertThat(accountRepository.findById(rollbackAccount.getId()).orElseThrow().getBalance()).isEqualByComparingTo(beforeRollback);

        var source = register("Concorrente Um", "c1@example.test", "c1");
        var destination = register("Concorrente Dois", "c2@example.test", "c2");
        pinService.create(source.response().user().id(), "123456");
        pixKeyService.create(destination.response().user().id(), new ApiDtos.PixKeyRequest(Enums.PixKeyType.EMAIL, "c2@example.test"));
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> calls = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                int index = i;
                calls.add(() -> {
                    try {
                        transferService.transfer(source.response().user().id(),
                                new ApiDtos.TransferRequest("c2@example.test", new BigDecimal("2000.00"), null, "123456"),
                                "concurrent-" + String.format("%08d", index));
                        return true;
                    } catch (ApiException exception) { return false; }
                });
            }
            long completed = executor.invokeAll(calls).stream().filter(future -> {
                try { return future.get(); } catch (Exception exception) { return false; }
            }).count();
            assertThat(completed).isEqualTo(5);
        } finally { executor.shutdownNow(); }
        BigDecimal sourceBalance = accountRepository.findByUserId(source.response().user().id()).orElseThrow().getBalance();
        BigDecimal destinationBalance = accountRepository.findByUserId(destination.response().user().id()).orElseThrow().getBalance();
        assertThat(sourceBalance).isEqualByComparingTo("40000.00");
        assertThat(destinationBalance).isEqualByComparingTo("60000.00");
        assertThat(sourceBalance.add(destinationBalance)).isEqualByComparingTo("100000.00");

        var admin = userRepository.findByUsername("admin").orElseThrow();
        assertThat(adminService.users(null, Pageable.unpaged())).isNotEmpty();
        assertThat(adminService.transfers(null, null, null, null, null, null, Pageable.unpaged())).isNotEmpty();
        assertThat(adminService.audits(null, Pageable.unpaged())).isNotEmpty();
        var brunoAccount = accountRepository.findByUserId(brunoId).orElseThrow();
        adminService.block(admin.getId(), brunoAccount.getId());
        assertThat(accountRepository.findById(brunoAccount.getId()).orElseThrow().getStatus()).isEqualTo(Enums.AccountStatus.TEMPORARILY_BLOCKED);
        adminService.unblock(admin.getId(), brunoAccount.getId());
        adminService.adjust(admin.getId(), brunoAccount.getId(), new ApiDtos.AdminAdjustmentRequest(new BigDecimal("25.00"), "ajuste de teste"));
        assertThat(transferRepository.findForAccount(accountRepository.findByUserId(aliceId).orElseThrow().getId(), Pageable.unpaged()).getTotalElements()).isEqualTo(1);
    }

    private AuthService.SessionTokens register(String name, String email, String username) {
        return authService.register(new ApiDtos.RegisterRequest(name, email, username, "SenhaForte123", "SenhaForte123", true), "JUnit");
    }
}
