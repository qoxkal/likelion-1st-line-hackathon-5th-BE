package com.line1.booktalk.domain.user;

import com.line1.booktalk.global.common.BaseEntity;
import com.line1.booktalk.global.common.IdGenerator;
import com.line1.booktalk.global.common.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보호자 계정. 로그인 없이 기기(deviceId) 단위로 게스트 계정을 만든다.
 * deviceId 원문은 저장하지 않고 해시값만 저장한다 (명세서 POST /auth/guest).
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @Column(length = 40)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType type;

    @Column(nullable = false, unique = true, length = 64)
    private String deviceIdHash;

    @Column(length = 30)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Language preferredLanguage;

    @Column(nullable = false)
    private boolean notificationEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    public static User createGuest(String deviceIdHash) {
        User user = new User();
        user.id = IdGenerator.generate(IdGenerator.USER);
        user.type = UserType.GUEST;
        user.deviceIdHash = deviceIdHash;
        user.notificationEnabled = false;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    /** PATCH /me: null이 아닌 값만 바꾼다 */
    public void update(String displayName, Language preferredLanguage, Boolean notificationEnabled) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (preferredLanguage != null) {
            this.preferredLanguage = preferredLanguage;
        }
        if (notificationEnabled != null) {
            this.notificationEnabled = notificationEnabled;
        }
    }
}
