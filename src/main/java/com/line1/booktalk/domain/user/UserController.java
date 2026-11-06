package com.line1.booktalk.domain.user;

import com.line1.booktalk.domain.user.dto.MeResponse;
import com.line1.booktalk.domain.user.dto.UpdateMeRequest;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 내 계정 조회 */
    @GetMapping
    public ApiResponse<MeResponse> getMe(@LoginUser String userId) {
        return ApiResponse.of(userService.getMe(userId));
    }

    /** 내 계정 수정 (보낸 필드만 변경) */
    @PatchMapping
    public ApiResponse<MeResponse> updateMe(@LoginUser String userId,
                                            @Valid @RequestBody UpdateMeRequest request) {
        return ApiResponse.of(userService.updateMe(userId, request));
    }
}
