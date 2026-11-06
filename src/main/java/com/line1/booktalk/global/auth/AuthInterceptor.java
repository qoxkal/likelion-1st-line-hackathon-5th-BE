package com.line1.booktalk.global.auth;

import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 인증이 필요한 API 앞에서 Authorization: Bearer {토큰}을 확인한다.
 * 해커톤에서는 Spring Security 대신 이 인터셉터 하나로 처리한다.
 * (인증 제외 경로는 WebConfig에서 관리)
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "loginUserId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 사전 요청(OPTIONS)에는 토큰이 없으므로 통과시킨다
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        String userId = jwtProvider.getUserId(token);
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
