package com.line1.booktalk.domain.upload;

import com.line1.booktalk.domain.book.BookUploadService;
import com.line1.booktalk.domain.book.ImageType;
import com.line1.booktalk.domain.book.dto.UploadPageResponse;
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
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class UploadController {
    private final UploadService service;

    @PostMapping("/uploads/image")
    ApiResponse<CreateUploadResponse> create(@LoginUser String userId, @Valid @RequestBody CreateUploadRequest r) {
        return ApiResponse.of(service.create(userId, r));
    }

    @PutMapping(value = "/uploads/{uploadId}/content", consumes = {"image/jpeg", "image/png", "image/webp", MediaType.APPLICATION_OCTET_STREAM_VALUE})
    ApiResponse<UploadStatusResponse> content(@LoginUser String userId, @PathVariable String uploadId,
                                              @RequestBody byte[] content, @RequestHeader(value = "Content-Type", required = false) String contentType) {
        return ApiResponse.of(service.content(userId, uploadId, content, contentType));
    }

    @PostMapping("/books/pages")
    ApiResponse<UploadPageResponse> page(@LoginUser String userId, @Valid @RequestBody RegisterPageRequest r) {
        return ApiResponse.of(service.registerPage(userId, r));
    }

    public record CreateUploadRequest(@NotBlank @Size(max = 255) String fileName, @NotBlank String contentType,
                                      @Positive @Max(15728640) long sizeBytes, @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String sha256) {}
    public record CreateUploadResponse(String uploadId, String uploadUrl, String method, Instant expiresAt) {}
    public record UploadStatusResponse(String uploadId, String status) {}
    public record RegisterPageRequest(@NotBlank String childId, String bookId, String bookTitle,
                                      @Size(max = 20) String pageLabel, @NotBlank String uploadId) {}
}

enum UploadStatus { PENDING, COMPLETED, CONSUMED }

@Getter @Entity @Table(name = "uploads") @NoArgsConstructor(access = AccessLevel.PROTECTED)
class Upload extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User user;
    @Column(nullable = false, length = 255) private String fileName;
    @Column(nullable = false, length = 30) private String contentType;
    @Column(nullable = false) private long sizeBytes;
    @Column(nullable = false, length = 64) private String sha256;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UploadStatus status;
    @Lob private byte[] content;
    @Column(nullable = false) private Instant expiresAt;
    static Upload create(User u, UploadController.CreateUploadRequest r) { Upload x = new Upload(); x.id = IdGenerator.generate(IdGenerator.UPLOAD); x.user = u; x.fileName = r.fileName(); x.contentType = r.contentType(); x.sizeBytes = r.sizeBytes(); x.sha256 = r.sha256().toLowerCase(); x.status = UploadStatus.PENDING; x.expiresAt = Instant.now().plusSeconds(600); return x; }
    void complete(byte[] content) { this.content = content; status = UploadStatus.COMPLETED; }
    byte[] consume() { if (status != UploadStatus.COMPLETED) throw new BusinessException(ErrorCode.INVALID_STATE, "업로드가 완료되지 않았어요."); status = UploadStatus.CONSUMED; byte[] bytes = content; content = null; return bytes; }
}
interface UploadRepository extends org.springframework.data.jpa.repository.JpaRepository<Upload, String> { Optional<Upload> findByIdAndUserId(String id, String userId); }

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
class UploadService {
    private final UserService userService; private final UploadRepository repository; private final BookUploadService bookUploadService;
    @Transactional UploadController.CreateUploadResponse create(String userId, UploadController.CreateUploadRequest r) {
        if (ImageType.fromContentType(r.contentType()).isEmpty()) throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        Upload u = repository.save(Upload.create(userService.getActiveUser(userId), r));
        return new UploadController.CreateUploadResponse(u.getId(), "/v1/uploads/" + u.getId() + "/content", "PUT", u.getExpiresAt());
    }
    @Transactional UploadController.UploadStatusResponse content(String userId, String id, byte[] bytes, String contentType) {
        Upload u = owned(userId, id); if (u.getExpiresAt().isBefore(Instant.now())) throw new BusinessException(ErrorCode.INVALID_STATE, "업로드 URL이 만료됐어요.");
        if (bytes.length != u.getSizeBytes() || !HashUtils.sha256(bytes).equalsIgnoreCase(u.getSha256())) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "파일 크기 또는 sha256이 일치하지 않아요.");
        if (ImageType.detect(bytes).isEmpty()) throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE); u.complete(bytes);
        return new UploadController.UploadStatusResponse(id, u.getStatus().name());
    }
    @Transactional UploadPageResponse registerPage(String userId, UploadController.RegisterPageRequest r) {
        Upload u = owned(userId, r.uploadId()); byte[] bytes = u.consume();
        try { return bookUploadService.uploadBytes(userId, r.childId(), r.bookId(), r.bookTitle(), r.pageLabel(), bytes); }
        catch (RuntimeException e) { throw e; }
    }
    private Upload owned(String userId, String id) { return repository.findByIdAndUserId(id, userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
}
