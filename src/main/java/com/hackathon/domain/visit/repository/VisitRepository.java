package com.hackathon.domain.visit.repository;

import com.hackathon.domain.visit.dto.response.PlaceVisitorCount;
import com.hackathon.domain.visit.entity.Visit;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    Optional<Visit> findByUserIdAndPlaceIdAndVisitedDate(String userId, Long placeId, LocalDate visitedDate);

    boolean existsByUserIdAndPlaceIdAndVisitedDateBefore(String userId, Long placeId, LocalDate visitedDate);

    long countByUserId(String userId);

    @Query("select distinct v.placeId from Visit v where v.userId = :userId")
    List<Long> findVisitedPlaceIds(@Param("userId") String userId);

    /** 한 장소에서 주어진 사용자들의 방문을 지운다. 샘플 방문을 다시 넣을 때만 쓴다 (실제 사용자 방문에는 쓰지 않는다) */
    @Transactional
    @Modifying
    @Query("delete from Visit v where v.placeId = :placeId and v.userId in :userIds")
    int deleteByPlaceIdAndUserIdIn(@Param("placeId") Long placeId, @Param("userIds") Collection<String> userIds);

    /** 한 사람이 여러 날 와도 1명으로 센다 */
    @Query("""
            select new com.hackathon.domain.visit.dto.response.PlaceVisitorCount(v.placeId, count(distinct v.userId))
            from Visit v
            where v.visitedDate between :from and :to
            group by v.placeId
            order by count(distinct v.userId) desc, v.placeId asc
            """)
    List<PlaceVisitorCount> findTopPlaces(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
