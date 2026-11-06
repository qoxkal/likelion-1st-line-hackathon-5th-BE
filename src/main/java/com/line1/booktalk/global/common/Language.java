package com.line1.booktalk.global.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 뜻풀이(설명) 언어. JSON에서는 명세서대로 "vi", "zh-CN" 같은 코드로 주고받고,
 * DB에는 VI, ZH_CN 같은 이름으로 저장한다.
 */
@Getter
@RequiredArgsConstructor
public enum Language {
    VI("vi"),
    ZH_CN("zh-CN"),
    TH("th"),
    EN("en"),
    JA("ja");

    private final String code;

    @JsonValue
    public String toJson() {
        return code;
    }

    @JsonCreator
    public static Language from(String code) {
        for (Language language : values()) {
            if (language.code.equalsIgnoreCase(code)) {
                return language;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 언어예요: " + code);
    }
}
