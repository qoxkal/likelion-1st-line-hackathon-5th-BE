package com.line1.booktalk.domain.auth;

import com.line1.booktalk.domain.auth.dto.GuestLoginRequest;
import com.line1.booktalk.domain.auth.dto.GuestLoginResponse;
import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.domain.user.UserRepository;
import com.line1.booktalk.global.auth.JwtProvider;
import com.line1.booktalk.global.common.HashUtils;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshSessionRepository refreshSessionRepository;
    private static final long REFRESH_TTL_SECONDS = 30L * 24 * 60 * 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 같은 기기면 기존 게스트 계정을, 처음이면 새 게스트 계정을 만들어 토큰을 발급한다.
     * deviceId 원문은 저장하지 않고 해시만 저장한다.
     */
    @Transactional
    public GuestLoginResponse loginAsGuest(GuestLoginRequest request) {
        String deviceIdHash = HashUtils.sha256(request.deviceId());

        User user = userRepository.findByDeviceIdHash(deviceIdHash)
                .orElseGet(() -> userRepository.save(User.createGuest(deviceIdHash)));

        TokenPair pair = issue(user);
        return new GuestLoginResponse(
                new GuestLoginResponse.UserSummary(user.getId(), user.getType().name()),
                pair.accessToken(), pair.refreshToken(),
                jwtProvider.getTtlSeconds()
        );
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        RefreshSession current = refreshSessionRepository.findByTokenHash(HashUtils.sha256(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!current.isUsable()) {
            throw new BusinessException(current.isRevoked() ? ErrorCode.REFRESH_TOKEN_REUSED : ErrorCode.TOKEN_EXPIRED);
        }
        current.revoke();
        return issue(current.getUser());
    }

    @Transactional
    public void logout(String userId, String refreshToken, boolean allDevices) {
        if (allDevices) {
            refreshSessionRepository.findAllByUserIdAndRevokedFalse(userId).forEach(RefreshSession::revoke);
            return;
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshSessionRepository.findByTokenHash(HashUtils.sha256(refreshToken))
                    .filter(s -> s.getUser().getId().equals(userId))
                    .ifPresent(RefreshSession::revoke);
        }
    }

    private TokenPair issue(User user) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshSessionRepository.save(RefreshSession.create(user, HashUtils.sha256(rawRefresh),
                Instant.now().plusSeconds(REFRESH_TTL_SECONDS)));
        return new TokenPair(jwtProvider.createAccessToken(user.getId(), user.getType().name()),
                rawRefresh, jwtProvider.getTtlSeconds());
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {}
}
