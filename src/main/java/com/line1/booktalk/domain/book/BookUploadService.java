package com.line1.booktalk.domain.book;

import com.line1.booktalk.domain.book.dto.UploadPageResponse;
import com.line1.booktalk.domain.child.Child;
import com.line1.booktalk.domain.child.ChildService;
import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.domain.user.UserService;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import com.line1.booktalk.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 사진 업로드와 페이지 등록을 한 번에 처리한다 (해커톤 단순화 2번).
 * 명세서의 POST /uploads/image + POST /books/pages 두 단계를 합친 것이다.
 */
@Service
@RequiredArgsConstructor
public class BookUploadService {

    private static final long MAX_BYTES = 4L * 1024 * 1024; // AI 서버 제한과 같게

    private final BookRepository bookRepository;
    private final BookPageRepository bookPageRepository;
    private final BookService bookService;
    private final UserService userService;
    private final ChildService childService;
    private final FileStorage fileStorage;

    @Transactional
    public UploadPageResponse upload(String userId, String childId, String bookId,
                                     String bookTitle, String pageLabel, MultipartFile file) {
        byte[] bytes = readBytes(file);
        return uploadBytes(userId, childId, bookId, bookTitle, pageLabel, bytes);
    }

    @Transactional
    public UploadPageResponse uploadBytes(String userId, String childId, String bookId,
                                          String bookTitle, String pageLabel, byte[] bytes) {
        User user = userService.getActiveUser(userId);
        Child child = childService.getOwnedChild(userId, childId);
        if (bytes == null || bytes.length == 0) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "사진 파일을 첨부해 주세요.");
        if (bytes.length > MAX_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        ImageType imageType = ImageType.detect(bytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE));

        // 2) 도서 정하기: bookId가 있으면 그 책, 없으면 bookTitle로 내 책장에 새 책을 만든다
        Book book = resolveBook(user, bookId, bookTitle);

        // 3) 사진 저장 (외부 공개 안 되는 서버 폴더)
        String storageKey = fileStorage.save("pages/" + userId, imageType.extension(), bytes);

        // 4) 페이지 등록. DB 저장이 실패하면 방금 저장한 사진도 지운다
        try {
            BookPage page = BookPage.create(
                    book, user, child,
                    normalize(pageLabel, 20),
                    storageKey,
                    imageType.contentType(),
                    bytes.length,
                    sha256(bytes)
            );
            return UploadPageResponse.from(bookPageRepository.save(page));
        } catch (RuntimeException e) {
            fileStorage.delete(storageKey);
            throw e;
        }
    }

    private Book resolveBook(User user, String bookId, String bookTitle) {
        if (bookId != null && !bookId.isBlank()) {
            return bookService.getVisibleBook(user.getId(), bookId);
        }
        String title = normalize(bookTitle, 100);
        if (title == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "bookId가 없으면 bookTitle을 입력해 주세요.");
        }
        return bookRepository.save(Book.createForUser(user, title));
    }

    private byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "사진 파일을 첨부해 주세요.");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "사진 파일을 읽지 못했어요.");
        }
    }

    /** 앞뒤 공백을 지우고, 비어 있으면 null. 너무 길면 오류 */
    private String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, maxLength + "자 이하로 입력해 주세요.");
        }
        return trimmed;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
