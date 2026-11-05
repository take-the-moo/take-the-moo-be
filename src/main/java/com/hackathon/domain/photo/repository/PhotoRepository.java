package com.hackathon.domain.photo.repository;

import com.hackathon.domain.photo.entity.Photo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, String> {

    /** 한 사용자의 사진. 오래된 순 (F8 나의 발자취, 인덱스 user_id, created_at) */
    List<Photo> findAllByUserIdOrderByCreatedAtAscIdAsc(String userId);
}
