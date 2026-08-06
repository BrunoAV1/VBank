package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.security.CurrentUser;
import dev.brunovasconcellos.vbank.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;
    public AdminController(AdminService service) { this.service = service; }

    @GetMapping("/users")
    Page<ApiDtos.AdminUserResponse> users(@RequestParam(required = false) String search,
                                          @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.users(search, pageable);
    }
    @GetMapping("/users/{id}")
    ApiDtos.AdminUserResponse user(@PathVariable UUID id) { return service.user(id); }
    @PatchMapping("/accounts/{id}/block")
    ApiDtos.AccountResponse block(Authentication auth, @PathVariable UUID id) { return service.block(CurrentUser.id(auth), id); }
    @PatchMapping("/accounts/{id}/unblock")
    ApiDtos.AccountResponse unblock(Authentication auth, @PathVariable UUID id) { return service.unblock(CurrentUser.id(auth), id); }
    @PostMapping("/accounts/{id}/adjustments")
    ApiDtos.AccountResponse adjust(Authentication auth, @PathVariable UUID id,
                                   @Valid @RequestBody ApiDtos.AdminAdjustmentRequest request) {
        return service.adjust(CurrentUser.id(auth), id, request);
    }
    @GetMapping("/transfers")
    Page<ApiDtos.TransferResponse> transfers(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) Enums.TransferStatus status,
                                              @RequestParam(required = false) BigDecimal minAmount,
                                              @RequestParam(required = false) BigDecimal maxAmount,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                              @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.transfers(search, status, minAmount, maxAmount, from, to, pageable);
    }
    @GetMapping("/audit-logs")
    Page<ApiDtos.AuditResponse> audits(@RequestParam(required = false) String search,
                                       @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.audits(search, pageable);
    }
}
