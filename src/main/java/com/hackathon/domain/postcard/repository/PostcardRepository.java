package com.hackathon.domain.postcard.repository;

import com.hackathon.domain.postcard.entity.Postcard;
import com.hackathon.domain.postcard.entity.PostcardStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostcardRepository extends JpaRepository<Postcard, String> {

    /** 서버가 다시 켜질 때 끝나지 못한 엽서를 FAILED로 바꾼다 */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Postcard p set p.status = com.hackathon.domain.postcard.entity.PostcardStatus.FAILED, "
            + "p.errorCode = :errorCode where p.status in :statuses")
    int failAll(@Param("statuses") Collection<PostcardStatus> statuses, @Param("errorCode") String errorCode);
}
