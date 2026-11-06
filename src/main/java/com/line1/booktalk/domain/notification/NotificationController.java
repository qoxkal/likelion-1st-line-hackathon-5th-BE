package com.line1.booktalk.domain.notification;

import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.domain.user.UserService;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.common.BaseEntity;
import com.line1.booktalk.global.common.HashUtils;
import com.line1.booktalk.global.common.IdGenerator;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping("/notification-settings")
    ApiResponse<SettingsResponse> get(@LoginUser String userId) { return ApiResponse.of(service.get(userId)); }

    @PatchMapping("/notification-settings")
    ApiResponse<SettingsResponse> update(@LoginUser String userId, @RequestBody UpdateSettingsRequest request) {
        return ApiResponse.of(service.update(userId, request));
    }

    @PostMapping("/devices")
    ApiResponse<DeviceResponse> device(@LoginUser String userId, @Valid @RequestBody DeviceRequest request) {
        return ApiResponse.of(service.register(userId, request));
    }

    public record SettingsResponse(boolean enabled, Set<Channel> channels, LocalTime reviewReminderTime, String timezone) {}
    public record UpdateSettingsRequest(Boolean enabled, Set<Channel> channels, LocalTime reviewReminderTime, String timezone) {}
    public record DeviceRequest(@NotNull Platform platform, @NotBlank String pushToken, @NotBlank String deviceId) {}
    public record DeviceResponse(String id, boolean registered) {}
}

enum Channel { PUSH, EMAIL }
enum Platform { IOS, ANDROID, WEB }

@Getter @Entity @Table(name = "notification_settings") @NoArgsConstructor(access = AccessLevel.PROTECTED)
class NotificationSettings extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", unique = true) private User user;
    @Column(nullable = false) private boolean enabled;
    @ElementCollection(fetch = FetchType.EAGER) @Enumerated(EnumType.STRING) private Set<Channel> channels = new HashSet<>();
    @Column(nullable = false) private LocalTime reviewReminderTime;
    @Column(nullable = false, length = 80) private String timezone;
    static NotificationSettings create(User u) { NotificationSettings s = new NotificationSettings(); s.id = "nfs_" + UUID.randomUUID().toString().replace("-", ""); s.user = u; s.reviewReminderTime = LocalTime.of(20, 0); s.timezone = "Asia/Seoul"; s.channels.add(Channel.PUSH); return s; }
    void update(Boolean enabled, Set<Channel> channels, LocalTime time, String timezone) { if (enabled != null) this.enabled = enabled; if (channels != null) this.channels = new HashSet<>(channels); if (time != null) this.reviewReminderTime = time; if (timezone != null) this.timezone = timezone; }
}
interface NotificationSettingsRepository extends org.springframework.data.jpa.repository.JpaRepository<NotificationSettings, String> { Optional<NotificationSettings> findByUserId(String userId); }

@Getter @Entity @Table(name = "push_devices", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id_hash"})) @NoArgsConstructor(access = AccessLevel.PROTECTED)
class PushDevice extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Platform platform;
    @Column(name = "device_id_hash", nullable = false, length = 64) private String deviceIdHash;
    @Column(nullable = false, length = 64) private String pushTokenHash;
    static PushDevice create(User u, NotificationController.DeviceRequest r) { PushDevice d = new PushDevice(); d.id = IdGenerator.generate(IdGenerator.DEVICE); d.user = u; d.platform = r.platform(); d.deviceIdHash = HashUtils.sha256(r.deviceId()); d.pushTokenHash = HashUtils.sha256(r.pushToken()); return d; }
    void update(NotificationController.DeviceRequest r) { platform = r.platform(); pushTokenHash = HashUtils.sha256(r.pushToken()); }
}
interface PushDeviceRepository extends org.springframework.data.jpa.repository.JpaRepository<PushDevice, String> { Optional<PushDevice> findByUserIdAndDeviceIdHash(String userId, String deviceIdHash); }

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
class NotificationService {
    private final UserService userService; private final NotificationSettingsRepository settingsRepository; private final PushDeviceRepository deviceRepository;
    @Transactional NotificationController.SettingsResponse get(String userId) { return response(findOrCreate(userId)); }
    @Transactional NotificationController.SettingsResponse update(String userId, NotificationController.UpdateSettingsRequest r) { if (r.timezone() != null) { try { ZoneId.of(r.timezone()); } catch (Exception e) { throw new BusinessException(ErrorCode.VALIDATION_ERROR, "올바른 IANA 시간대를 입력해 주세요."); } } NotificationSettings s = findOrCreate(userId); s.update(r.enabled(), r.channels(), r.reviewReminderTime(), r.timezone()); return response(s); }
    @Transactional NotificationController.DeviceResponse register(String userId, NotificationController.DeviceRequest r) { User u = userService.getActiveUser(userId); String hash = HashUtils.sha256(r.deviceId()); PushDevice d = deviceRepository.findByUserIdAndDeviceIdHash(userId, hash).orElseGet(() -> deviceRepository.save(PushDevice.create(u, r))); d.update(r); return new NotificationController.DeviceResponse(d.getId(), true); }
    private NotificationSettings findOrCreate(String userId) { return settingsRepository.findByUserId(userId).orElseGet(() -> settingsRepository.save(NotificationSettings.create(userService.getActiveUser(userId)))); }
    private NotificationController.SettingsResponse response(NotificationSettings s) { return new NotificationController.SettingsResponse(s.isEnabled(), Set.copyOf(s.getChannels()), s.getReviewReminderTime(), s.getTimezone()); }
}
