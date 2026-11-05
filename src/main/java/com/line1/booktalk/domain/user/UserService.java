package com.line1.booktalk.domain.user;

import com.line1.booktalk.domain.user.dto.MeResponse;
import com.line1.booktalk.domain.user.dto.UpdateMeRequest;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public MeResponse getMe(String userId) {
        return MeResponse.from(getActiveUser(userId));
    }

    @Transactional
    public MeResponse updateMe(String userId, UpdateMeRequest request) {
        User user = getActiveUser(userId);
        user.update(
                request.displayName() == null ? null : request.displayName().trim(),
                request.preferredLanguage(),
                request.notificationEnabled()
        );
        return MeResponse.from(user);
    }

    /**
     * 토큰은 올바른데 사용자가 없는 경우(예: 로컬 H2 DB를 재시작한 경우)도 401로 처리한다.
     * 그러면 프론트가 /auth/guest를 다시 호출해서 새 토큰을 받으면 된다.
     * 다른 서비스에서도 사용자 엔티티가 필요할 때 이 메서드를 쓴다.
     */
    public User getActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
