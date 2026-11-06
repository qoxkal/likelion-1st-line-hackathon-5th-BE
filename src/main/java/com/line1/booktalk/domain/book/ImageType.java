package com.line1.booktalk.domain.book;

import java.util.Optional;

/**
 * 업로드를 허용하는 사진 형식.
 * 파일 이름이나 Content-Type은 바꿔서 보낼 수 있으므로, 파일 앞부분(매직 넘버)을 직접 확인한다.
 */
public enum ImageType {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String contentType;
    private final String extension;

    ImageType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageType> detect(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return Optional.of(PNG);
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    public static Optional<ImageType> fromContentType(String contentType) {
        for (ImageType value : values()) if (value.contentType.equalsIgnoreCase(contentType)) return Optional.of(value);
        return Optional.empty();
    }
}
