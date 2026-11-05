package com.line1.booktalk.domain.child.dto;

import com.line1.booktalk.domain.child.Child;
import com.line1.booktalk.domain.child.Relationship;
import com.line1.booktalk.global.common.Language;

/** 아이 프로필 응답 */
public record ChildResponse(
        String id,
        String nickname,
        int age,
        Relationship relationship,
        Language explanationLanguage
) {
    public static ChildResponse from(Child child) {
        return new ChildResponse(
                child.getId(),
                child.getNickname(),
                child.getAge(),
                child.getRelationship(),
                child.getExplanationLanguage()
        );
    }
}
