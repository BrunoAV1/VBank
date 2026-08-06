package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.security.CurrentUser;
import dev.brunovasconcellos.vbank.service.FundingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sandbox/funding")
public class FundingController {
    private final FundingService service;
    public FundingController(FundingService service) { this.service = service; }
    @GetMapping("/status")
    ApiDtos.FundingStatusResponse status(Authentication auth) { return service.status(CurrentUser.id(auth)); }
    @PostMapping
    ApiDtos.FundingResponse fund(Authentication auth) { return service.fund(CurrentUser.id(auth)); }
}

