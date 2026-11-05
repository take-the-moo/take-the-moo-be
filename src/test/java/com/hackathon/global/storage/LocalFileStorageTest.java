package com.hackathon.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    Path root;

    @Test
    void 저장한_파일을_이름으로_다시_읽는다() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(root.resolve("uploads"));
        byte[] content = {1, 2, 3};

        String name = storage.save(content, "jpg");

        assertThat(name).matches("[0-9a-f-]{36}\\.jpg");
        assertThat(storage.load(name).getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    void 없는_파일이나_형식이_다른_이름은_NOT_FOUND() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(root);
        Files.writeString(root.getParent().resolve("secret.txt"), "x");

        assertThatThrownBy(() -> storage.load("00000000-0000-0000-0000-000000000000.jpg"))
                .isInstanceOf(StorageException.class)
                .extracting(e -> ((StorageException) e).getErrorCode()).isEqualTo(StorageErrorType.NOT_FOUND);
        assertThatThrownBy(() -> storage.load("../secret.txt"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void 허용하지_않는_확장자는_저장하지_않는다() {
        LocalFileStorage storage = new LocalFileStorage(root);

        assertThatThrownBy(() -> storage.save(new byte[]{1}, "exe")).isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storage.save(new byte[]{1}, "../jpg")).isInstanceOf(StorageException.class);
    }

    @Test
    void 지우면_다시_읽을_수_없다() {
        LocalFileStorage storage = new LocalFileStorage(root);
        String name = storage.save(new byte[]{1}, "png");

        storage.delete(name);
        storage.delete("../secret.txt");

        assertThatThrownBy(() -> storage.load(name)).isInstanceOf(StorageException.class);
    }

    @Test
    void 업로드_형식에서_확장자를_고른다() {
        assertThat(FileStorage.extensionOf("image/JPEG")).isEqualTo("jpg");
        assertThat(FileStorage.extensionOf("image/webp")).isEqualTo("webp");
        assertThat(FileStorage.extensionOf("image/heic")).isNull();
    }
}
