package com.hackathon.global.storage;

import com.hackathon.global.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * app.storage.dir(기본 ./uploads)에 파일을 둔다.
 * - 로컬 실행: 프로젝트 폴더의 uploads/ (gitignore)
 * - 도커: 컨테이너의 /app/uploads → compose.yml의 uploads-data 볼륨이라 컨테이너를 새로 만들어도 남는다
 * 이름은 이 클래스가 만든 형식(UUID.확장자)만 받는다. 요청 값으로 "../" 같은 경로를 넘겨 다른 파일을 읽지 못하게 한다.
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final Pattern STORED_NAME = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");
    private static final Pattern EXTENSION = Pattern.compile("jpg|png|webp");

    private final Path root;

    @Autowired
    public LocalFileStorage(AppProperties properties) {
        this(Path.of(properties.storage().dir()));
    }

    public LocalFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String save(byte[] content, String extension) {
        if (extension == null || !EXTENSION.matcher(extension).matches()) {
            throw new StorageException(StorageErrorType.WRITE_FAILED, "허용하지 않는 확장자: " + extension);
        }
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(storedName), content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new StorageException(StorageErrorType.WRITE_FAILED,
                    "파일 저장 실패: " + storedName + ", " + e.getClass().getSimpleName(), e);
        }
        return storedName;
    }

    @Override
    public Resource load(String storedName) {
        Path path = resolve(storedName);
        if (path == null || !Files.isRegularFile(path)) {
            throw new StorageException(StorageErrorType.NOT_FOUND, "파일 없음: " + storedName);
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storedName) {
        Path path = resolve(storedName);
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("파일 삭제 실패: name={}, error={}", storedName, e.getClass().getSimpleName());
        }
    }

    private Path resolve(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) return null;
        return root.resolve(storedName);
    }
}
