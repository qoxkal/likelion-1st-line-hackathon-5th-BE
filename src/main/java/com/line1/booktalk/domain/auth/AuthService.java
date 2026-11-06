package com.line1.booktalk.domain.auth;

import com.line1.booktalk.domain.auth.dto.GuestLoginRequest;
import com.line1.booktalk.domain.auth.dto.GuestLoginResponse;
import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.domain.user.UserRepository;
import com.line1.booktalk.global.auth.JwtProvider;
import com.line1.booktalk.global.common.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 같은 기기면 기존 게스트 계정을, 처음이면 새 게스트 계정을 만들어 토큰을 발급한다.
     * deviceId 원문은 저장하지 않고 해시만 저장한다.
     */
    @Transactional
    public GuestLoginResponse loginAsGuest(GuestLoginRequest request) {
        String deviceIdHash = HashUtils.sha256(request.deviceId());

        User user = userRepository.findByDeviceIdHash(deviceIdHash)
                .orElseGet(() -> userRepository.save(User.createGuest(deviceIdHash)));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getType().name());

        return new GuestLoginResponse(
                new GuestLoginResponse.UserSummary(user.getId(), user.getType().name()),
                accessToken,
                jwtProvider.getTtlSeconds()
        );
    }
}
