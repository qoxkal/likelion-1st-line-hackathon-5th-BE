package com.line1.booktalk.domain.child.dto;

import com.line1.booktalk.domain.child.Relationship;
import com.line1.booktalk.global.common.Language;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH /children/{childId} 요청. 보낸 필드만 바꾼다. */
public record UpdateChildRequest(
        @Size(min = 1, max = 20, message = "별칭은 1~20자여야 해요.")
        @Pattern(regexp = ".*\\S.*", message = "별칭을 입력해 주세요.")
        String nickname,

        @Min(value = 3, message = "나이는 3 이상 7 이하여야 해요.")
        @Max(value = 7, message = "나이는 3 이상 7 이하여야 해요.")
        Integer age,

        Relationship relationship,

        Language explanationLanguage
) {
}
