package com.hackathon.domain.review.repository;

import com.hackathon.domain.review.entity.Review;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 최신순 (인덱스 place_id, created_at). 같은 시각이면 id가 큰 것이 먼저 */
    List<Review> findByPlaceIdOrderByCreatedAtDescIdDesc(Long placeId, Pageable pageable);
}
