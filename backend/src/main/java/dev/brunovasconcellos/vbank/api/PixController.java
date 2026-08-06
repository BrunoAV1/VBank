package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.security.CurrentUser;
import dev.brunovasconcellos.vbank.service.PixKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pix")
public class PixController {
    private final PixKeyService service;
    public PixController(PixKeyService service) { this.service = service; }

    @GetMapping("/keys")
    List<ApiDtos.PixKeyResponse> list(Authentication auth) { return service.list(CurrentUser.id(auth)); }

    @PostMapping("/keys")
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.PixKeyResponse create(Authentication auth, @Valid @RequestBody ApiDtos.PixKeyRequest request) {
        return service.create(CurrentUser.id(auth), request);
    }

    @DeleteMapping("/keys/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication auth, @PathVariable UUID id) { service.delete(CurrentUser.id(auth), id); }

    @PostMapping("/resolve")
    ApiDtos.ResolvedPixKeyResponse resolve(Authentication auth, @Valid @RequestBody ApiDtos.ResolvePixKeyRequest request) {
        return service.resolve(CurrentUser.id(auth), request.key());
    }
}

