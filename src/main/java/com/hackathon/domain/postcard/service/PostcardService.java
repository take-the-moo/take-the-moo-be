package com.hackathon.domain.postcard.service;

import com.hackathon.domain.photo.service.PhotoService;
import com.hackathon.domain.postcard.dto.request.PostcardCreateRequestDto;
import com.hackathon.domain.postcard.dto.response.PostcardResponseDto;
import com.hackathon.domain.postcard.entity.Postcard;
import com.hackathon.domain.postcard.entity.PostcardStatus;
import com.hackathon.domain.postcard.entity.PostcardTemplate;
import com.hackathon.domain.postcard.exception.PostcardErrorType;
import com.hackathon.domain.postcard.exception.PostcardException;
import com.hackathon.domain.postcard.repository.PostcardRepository;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.storage.FileStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * F7 회고 엽서. 요청은 바로 PENDING으로 저장하고 돌려준다. 만드는 일은 PostcardGenerator가 백그라운드에서 한다.
 * 프론트는 GET /api/postcards/{id}로 상태를 2초쯤마다 확인하고, COMPLETED면 imageUrl을 보여 준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostcardService {

    static final String INTERRUPTED = "INTERRUPTED";

    private final PostcardRepository postcardRepository;
    private final PostcardGenerator generator;
    private final UserService userService;
    private final PhotoService photoService;
    private final FileStorage fileStorage;

    /** 없는 사용자 404 USER_NOT_FOUND, 남의 사진 403 PHOTO_FORBIDDEN, 없는 사진 404 PHOTO_NOT_FOUND */
    public PostcardResponseDto create(PostcardCreateRequestDto request) {
        String userId = userService.getUser(request.userId()).userId();
        String photoId = photoService.getOwnedPhoto(userId, request.photoId().strip()).photoId();
        PostcardTemplate template = request.template() != null ? request.template() : PostcardTemplate.BASIC;

        // save가 커밋된 뒤에 예약해야 백그라운드에서 행을 찾는다 (이 메서드에는 트랜잭션을 걸지 않는다)
        Postcard postcard = postcardRepository.save(
                Postcard.create(userId, photoId, request.comment().strip(), template));
        generator.submit(postcard.getId());
        return PostcardResponseDto.from(postcard);
    }

    /** 없으면 404 POSTCARD_NOT_FOUND */
    public PostcardResponseDto get(String postcardId) {
        return PostcardResponseDto.from(find(postcardId));
    }

    /** 완성된 엽서 이미지(JPEG). 아직이거나 실패했으면 409 POSTCARD_NOT_READY */
    public Resource getImage(String postcardId) {
        Postcard postcard = find(postcardId);
        if (postcard.getStatus() != PostcardStatus.COMPLETED) {
            throw new PostcardException(PostcardErrorType.NOT_READY,
                    "postcardId=" + postcardId + ", status=" + postcard.getStatus());
        }
        return fileStorage.load(postcard.getImageName());
    }

    /** 서버가 다시 켜지면 만들던 엽서는 이어서 만들 수 없다 → FAILED(INTERRUPTED). 프론트는 다시 요청하게 한다 */
    @EventListener(ApplicationReadyEvent.class)
    public void failInterrupted() {
        int count = postcardRepository.failAll(List.of(PostcardStatus.PENDING, PostcardStatus.PROCESSING), INTERRUPTED);
        if (count > 0) log.info("재시작으로 끝나지 못한 엽서를 실패 처리했습니다: count={}", count);
    }

    private Postcard find(String postcardId) {
        return postcardRepository.findById(postcardId)
                .orElseThrow(() -> new PostcardException(PostcardErrorType.NOT_FOUND, "postcardId=" + postcardId));
    }
}
