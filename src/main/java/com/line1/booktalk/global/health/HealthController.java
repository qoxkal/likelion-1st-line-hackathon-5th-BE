package com.line1.booktalk.global.health;

import com.line1.booktalk.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 서버가 살아 있는지 확인하는 용도.
 * 배포 후 서버 깨우기(무료 플랜 대비)에도 쓸 수 있다.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.of(Map.of("status", "ok"));
    }
}
