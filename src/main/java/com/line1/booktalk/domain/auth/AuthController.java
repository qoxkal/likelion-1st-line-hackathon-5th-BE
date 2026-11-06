package com.line1.booktalk.domain.auth;

import com.line1.booktalk.domain.auth.dto.GuestLoginRequest;
import com.line1.booktalk.domain.auth.dto.GuestLoginResponse;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 게스트 세션 생성 (인증 불필요) */
    @PostMapping("/guest")
    public ApiResponse<GuestLoginResponse> guest(@Valid @RequestBody GuestLoginRequest request) {
        return ApiResponse.of(authService.loginAsGuest(request));
    }
}
