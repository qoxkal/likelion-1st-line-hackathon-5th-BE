package com.line1.booktalk.domain.child;

import com.line1.booktalk.domain.child.dto.ChildResponse;
import com.line1.booktalk.domain.child.dto.CreateChildRequest;
import com.line1.booktalk.domain.child.dto.UpdateChildRequest;
import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.domain.user.UserService;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    /** 보호자 한 명이 만들 수 있는 아이 프로필 수 */
    private static final int MAX_CHILDREN_PER_USER = 5;

    private final ChildRepository childRepository;
    private final UserService userService;

    public List<ChildResponse> getChildren(String userId) {
        return childRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(ChildResponse::from)
                .toList();
    }

    @Transactional
    public ChildResponse createChild(String userId, CreateChildRequest request) {
        User user = userService.getActiveUser(userId);

        if (childRepository.countByUserId(userId) >= MAX_CHILDREN_PER_USER) {
            throw new BusinessException(ErrorCode.CHILD_LIMIT_EXCEEDED);
        }

        Child child = Child.create(
                user,
                request.nickname().trim(),
                request.age(),
                request.relationship(),
                request.explanationLanguage()
        );
        return ChildResponse.from(childRepository.save(child));
    }

    @Transactional
    public ChildResponse updateChild(String userId, String childId, UpdateChildRequest request) {
        Child child = getOwnedChild(userId, childId);
        child.update(
                request.nickname() == null ? null : request.nickname().trim(),
                request.age(),
                request.relationship(),
                request.explanationLanguage()
        );
        return ChildResponse.from(child);
    }

    /**
     * 소유권 검사. 내 아이가 아니면 "없는 아이"처럼 404를 준다.
     * (403을 주면 그 id가 존재한다는 사실이 드러나기 때문, 명세서 AUTHZ-01)
     * 페이지 분석처럼 childId를 받는 다른 서비스에서도 이 메서드를 쓴다.
     */
    public Child getOwnedChild(String userId, String childId) {
        return childRepository.findByIdAndUserId(childId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /** 대화 세션 교체처럼 같은 아이 기준으로 직렬화가 필요한 작업에서 사용한다. */
    @Transactional
    public Child getOwnedChildForUpdate(String userId, String childId) {
        return childRepository.findOwnedByIdForUpdate(childId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public void deleteChild(String userId, String childId, String confirmation) {
        if (!"DELETE_CHILD_PROFILE".equals(confirmation)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "삭제 확인 문구가 올바르지 않아요.");
        }
        childRepository.delete(getOwnedChild(userId, childId));
    }
}
