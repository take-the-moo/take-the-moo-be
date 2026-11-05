package com.hackathon.domain.postcard.service;

import com.hackathon.domain.photo.service.PhotoFile;
import com.hackathon.domain.photo.service.PhotoInfo;
import com.hackathon.domain.photo.service.PhotoService;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.postcard.entity.Postcard;
import com.hackathon.domain.postcard.entity.PostcardStatus;
import com.hackathon.domain.postcard.render.PostcardRenderer;
import com.hackathon.domain.postcard.repository.PostcardRepository;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.exception.DomainException;
import com.hackathon.global.storage.FileStorage;
import com.hackathon.global.storage.StorageErrorType;
import com.hackathon.global.util.SeoulTime;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 엽서를 백그라운드에서 만든다: PROCESSING → 제목(AI) → 원본 사진 + 제목 + 한 문장을 템플릿에 합성 → COMPLETED / FAILED.
 * - 사진은 원본 그대로 쓴다 (AI로 다시 그리지 않는다). 휴대폰 사진의 회전 정보(EXIF)는 합성할 때 반영한다
 * - 제목 AI가 실패하면 기본 제목으로 만든다. 시연이 끊기지 않게 COMPLETED로 끝낸다
 * - FAILED는 사진 파일이 없거나 사진을 읽지 못하는 것처럼 엽서를 아예 만들 수 없을 때만 쓴다 (error_code에 사유)
 * - 동시에 2개까지만 만든다 (큰 사진을 읽을 때 메모리). 공통 Executor 빈을 만들지 않고 여기서 직접 둔다 —
 *   Executor 빈이 생기면 스프링 기본 실행기(SSE 스트리밍이 씀)가 꺼질 수 있다
 */
@Slf4j
@Component
public class PostcardGenerator {

    private static final int CONCURRENCY = 2;

    private final PostcardRepository postcardRepository;
    private final PhotoService photoService;
    private final PlaceService placeService;
    private final UserService userService;
    private final PostcardTitleWriter titleWriter;
    private final PostcardRenderer renderer;
    private final FileStorage fileStorage;
    private final ExecutorService executor =
            Executors.newFixedThreadPool(CONCURRENCY, Thread.ofVirtual().name("postcard-", 0).factory());

    public PostcardGenerator(PostcardRepository postcardRepository, PhotoService photoService,
                             PlaceService placeService, UserService userService,
                             PostcardTitleWriter titleWriter, PostcardRenderer renderer,
                             FileStorage fileStorage) {
        this.postcardRepository = postcardRepository;
        this.photoService = photoService;
        this.placeService = placeService;
        this.userService = userService;
        this.titleWriter = titleWriter;
        this.renderer = renderer;
        this.fileStorage = fileStorage;
    }

    /** 생성을 예약한다. 엽서 행이 저장(커밋)된 뒤에 부른다 */
    public void submit(String postcardId) {
        executor.execute(() -> generate(postcardId));
    }

    void generate(String postcardId) {
        Postcard postcard = postcardRepository.findById(postcardId).orElse(null);
        if (postcard == null || postcard.getStatus() != PostcardStatus.PENDING) return;
        long start = System.nanoTime();
        postcard.startProcessing();
        postcard = postcardRepository.save(postcard);

        try {
            PhotoInfo photo = photoService.getPhoto(postcard.getPhotoId());
            PhotoFile file = photoService.getFile(photo.photoId());
            byte[] original = file.resource().getContentAsByteArray();

            Language language = userService.getUser(postcard.getUserId()).language();
            String title = titleWriter.write(SeoulTime.today(), placeName(photo.placeId()), language);

            byte[] jpeg = renderer.render(postcard.getTemplate(), original, title, postcard.getComment());
            postcard.complete(fileStorage.save(jpeg, "jpg"));
            postcardRepository.save(postcard);
            log.info("엽서 완료: postcardId={}, template={}, bytes={}, latencyMs={}",
                    postcardId, postcard.getTemplate(), jpeg.length, elapsedMs(start));
        } catch (DomainException e) {
            fail(postcard, e.getErrorCode() == StorageErrorType.NOT_FOUND ? "PHOTO_FILE_MISSING"
                    : e.getErrorCode().getCode(), e);
        } catch (IOException | RuntimeException e) {
            fail(postcard, "RENDER_FAILED", e);
        }
    }

    private String placeName(Long placeId) {
        if (placeId == null) return null;
        try {
            return placeService.getActivePlace(placeId).getName();
        } catch (DomainException e) {
            return null;   // 그사이 노출이 꺼진 장소면 기본 장소(서대문)로 쓴다
        }
    }

    private void fail(Postcard postcard, String errorCode, Exception e) {
        log.warn("엽서 실패: postcardId={}, errorCode={}, error={}", postcard.getId(), errorCode, e.getClass().getSimpleName());
        try {
            postcard.fail(errorCode);
            postcardRepository.save(postcard);
        } catch (RuntimeException saveError) {
            log.error("엽서 실패 상태 저장 실패: postcardId={}", postcard.getId(), saveError);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
