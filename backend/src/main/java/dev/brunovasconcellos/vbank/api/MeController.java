package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.security.CurrentUser;
import dev.brunovasconcellos.vbank.service.AuthService;
import dev.brunovasconcellos.vbank.service.PinService;
import dev.brunovasconcellos.vbank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final UserService userService;
    private final PinService pinService;
    private final AuthService authService;

    public MeController(UserService userService, PinService pinService, AuthService authService) {
        this.userService = userService;
        this.pinService = pinService;
        this.authService = authService;
    }

    @GetMapping
    ApiDtos.UserResponse get(Authentication authentication) { return userService.get(CurrentUser.id(authentication)); }

    @PatchMapping
    ApiDtos.UserResponse update(Authentication authentication, @Valid @RequestBody ApiDtos.UpdateProfileRequest request) {
        return userService.update(CurrentUser.id(authentication), request.fullName());
    }

    @PatchMapping("/password")
    ApiDtos.SessionResponse password(Authentication authentication, @Valid @RequestBody ApiDtos.ChangePasswordRequest request) {
        userService.changePassword(CurrentUser.id(authentication), request);
        return new ApiDtos.SessionResponse("Senha alterada. Entre novamente em todos os dispositivos.");
    }

    @PostMapping("/pin")
    ApiDtos.SessionResponse createPin(Authentication authentication, @Valid @RequestBody ApiDtos.CreatePinRequest request) {
        pinService.create(CurrentUser.id(authentication), request.pin());
        return new ApiDtos.SessionResponse("PIN criado com segurança.");
    }

    @PatchMapping("/pin")
    ApiDtos.SessionResponse changePin(Authentication authentication, @Valid @RequestBody ApiDtos.ChangePinRequest request) {
        pinService.change(CurrentUser.id(authentication), request.currentPin(), request.newPin());
        return new ApiDtos.SessionResponse("PIN alterado.");
    }

    @GetMapping("/sessions")
    List<UserService.SessionInfo> sessions(Authentication authentication) {
        return userService.sessions(CurrentUser.id(authentication));
    }

    @PostMapping("/block")
    ApiDtos.SessionResponse block(Authentication authentication) {
        userService.selfBlock(CurrentUser.id(authentication));
        return new ApiDtos.SessionResponse("Conta temporariamente bloqueada e sessões encerradas.");
    }

    @PostMapping("/logout-all")
    ResponseEntity<ApiDtos.SessionResponse> logoutAll(Authentication authentication) {
        authService.logoutAll(CurrentUser.id(authentication));
        return ResponseEntity.ok(new ApiDtos.SessionResponse("Todas as sessões foram encerradas."));
    }
}

