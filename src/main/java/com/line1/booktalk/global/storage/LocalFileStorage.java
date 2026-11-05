package com.line1.booktalk.global.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 서버 폴더에 사진을 저장한다. 외부에서 URL로 접근할 수 없는 곳이라 공개되지 않는다.
 * key 예시: pages/usr_xxx/3f2a....jpg
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.local-dir}") String rootDir) {
        this.root = Path.of(rootDir).toAbsolutePath().normalize();
    }

    @Override
    public String save(String directory, String extension, byte[] content) {
        String key = directory + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("사진 저장에 실패했습니다.", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 읽지 못했습니다.", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            // 삭제 실패로 요청 전체를 실패시키지는 않는다. key만 기록한다 (사진 내용은 기록하지 않음)
            log.warn("사진 삭제 실패: {}", key);
        }
    }

    /** key가 저장 폴더 밖을 가리키지 못하게 막는다 (예: ../../etc/passwd) */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 저장 경로입니다.");
        }
        return path;
    }
}
