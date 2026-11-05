package com.hackathon.domain.review.repository;

import com.hackathon.domain.review.entity.Review;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 최신순 (인덱스 place_id, created_at). 같은 시각이면 id가 큰 것이 먼저 */
    List<Review> findByPlaceIdOrderByCreatedAtDescIdDesc(Long placeId, Pageable pageable);

    /** 샘플 한걸음이 이미 들어간 장소인지 (샘플 사용자가 쓴 글이 있는지) */
    boolean existsByPlaceIdAndUserIdIn(Long placeId, Collection<String> userIds);

    /** 샘플 한걸음을 며칠 전에 쓴 것으로 보이게 한다. 실제 사용자 글에는 쓰지 않는다 */
    @Transactional
    @Modifying
    @Query("update Review r set r.createdAt = :writtenAt, r.updatedAt = :writtenAt where r.id = :id")
    int backdate(@Param("id") Long id, @Param("writtenAt") LocalDateTime writtenAt);
}
