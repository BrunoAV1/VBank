package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.security.CurrentUser;
import dev.brunovasconcellos.vbank.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    Page<ApiDtos.NotificationResponse> list(Authentication auth,
                                             @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.list(CurrentUser.id(auth), pageable);
    }
    @PatchMapping("/{id}/read")
    ApiDtos.NotificationResponse read(Authentication auth, @PathVariable UUID id) { return service.read(CurrentUser.id(auth), id); }
    @PatchMapping("/read-all")
    ApiDtos.SessionResponse readAll(Authentication auth) {
        int count = service.readAll(CurrentUser.id(auth));
        return new ApiDtos.SessionResponse(count + " notificações marcadas como lidas.");
    }
}

