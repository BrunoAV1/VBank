package dev.brunovasconcellos.vbank.integration;

import dev.brunovasconcellos.vbank.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RollbackProbe {
    private final AccountRepository repository;
    public RollbackProbe(AccountRepository repository) { this.repository = repository; }

    @Transactional
    public void changeThenFail(UUID accountId) {
        var account = repository.findLockedById(accountId).orElseThrow();
        account.setBalance(account.getBalance().subtract(new BigDecimal("123.45")));
        throw new IllegalStateException("falha deliberada para testar rollback");
    }
}

