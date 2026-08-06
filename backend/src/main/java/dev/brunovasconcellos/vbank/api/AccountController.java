package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.security.CurrentUser;
import dev.brunovasconcellos.vbank.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }

    @GetMapping
    ApiDtos.AccountResponse get(Authentication auth) { return service.get(CurrentUser.id(auth)); }

    @GetMapping("/balance")
    ApiDtos.BalanceResponse balance(Authentication auth) { return service.balance(CurrentUser.id(auth)); }

    @GetMapping("/statement")
    Page<ApiDtos.LedgerEntryResponse> statement(
            Authentication auth,
            @RequestParam(required = false) Enums.LedgerType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.statement(CurrentUser.id(auth), type, from, to, minAmount, maxAmount, search, pageable);
    }

    @GetMapping("/dashboard")
    ApiDtos.DashboardResponse dashboard(Authentication auth) { return service.dashboard(CurrentUser.id(auth)); }
}
