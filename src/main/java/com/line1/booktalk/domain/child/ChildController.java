package com.line1.booktalk.domain.child;

import com.line1.booktalk.domain.child.dto.ChildResponse;
import com.line1.booktalk.domain.child.dto.CreateChildRequest;
import com.line1.booktalk.domain.child.dto.UpdateChildRequest;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildService childService;

    /** 내 아이 프로필 목록 */
    @GetMapping
    public ApiResponse<List<ChildResponse>> getChildren(@LoginUser String userId) {
        return ApiResponse.of(childService.getChildren(userId));
    }

    /** 아이 프로필 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChildResponse> createChild(@LoginUser String userId,
                                                  @Valid @RequestBody CreateChildRequest request) {
        return ApiResponse.of(childService.createChild(userId, request));
    }

    /** 아이 프로필 수정 (보낸 필드만 변경) */
    @PatchMapping("/{childId}")
    public ApiResponse<ChildResponse> updateChild(@LoginUser String userId,
                                                  @PathVariable String childId,
                                                  @Valid @RequestBody UpdateChildRequest request) {
        return ApiResponse.of(childService.updateChild(userId, childId, request));
    }
}
