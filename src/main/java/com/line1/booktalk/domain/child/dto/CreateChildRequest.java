package com.line1.booktalk.domain.child.dto;

import com.line1.booktalk.domain.child.Relationship;
import com.line1.booktalk.global.common.Language;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** POST /children 요청. 아이 실명 대신 별칭을 권장한다. */
public record CreateChildRequest(
        @NotBlank(message = "아이 별칭을 입력해 주세요.")
        @Size(max = 20, message = "별칭은 20자 이하여야 해요.")
        String nickname,

        @NotNull(message = "나이를 선택해 주세요.")
        @Min(value = 3, message = "나이는 3 이상 7 이하여야 해요.")
        @Max(value = 7, message = "나이는 3 이상 7 이하여야 해요.")
        Integer age,

        @NotNull(message = "아이와의 관계를 선택해 주세요.")
        Relationship relationship,

        @NotNull(message = "뜻풀이 언어를 선택해 주세요.")
        Language explanationLanguage
) {
}
