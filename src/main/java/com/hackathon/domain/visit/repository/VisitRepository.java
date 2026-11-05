package com.hackathon.domain.visit.repository;

import com.hackathon.domain.visit.dto.response.PlaceVisitor;
import com.hackathon.domain.visit.dto.response.PlaceVisitorCount;
import com.hackathon.domain.visit.entity.Visit;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    Optional<Visit> findByUserIdAndPlaceIdAndVisitedDate(String userId, Long placeId, LocalDate visitedDate);

    boolean existsByUserIdAndPlaceIdAndVisitedDateBefore(String userId, Long placeId, LocalDate visitedDate);

    long countByUserId(String userId);

    @Query("select distinct v.placeId from Visit v where v.userId = :userId")
    List<Long> findVisitedPlaceIds(@Param("userId") String userId);

    /** 주어진 장소들을 다녀간 사람 (장소 × 사람 중복 없이). F6 흔적 지도의 방문자 수·국적 분포 */
    @Query("""
            select distinct new com.hackathon.domain.visit.dto.response.PlaceVisitor(v.placeId, v.userId)
            from Visit v
            where v.placeId in :placeIds
            """)
    List<PlaceVisitor> findPlaceVisitors(@Param("placeIds") Collection<Long> placeIds);

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
