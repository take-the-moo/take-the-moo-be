package com.hackathon.domain.visit.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방문 도장. 하루에 한 장소당 하나다. 사용자 위치는 저장하지 않는다 (docs/db.md).
 * 다른 도메인은 연관관계 대신 userId·placeId로 참조한다.
 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_visit_user_place_date", columnNames = {"userId", "placeId", "visitedDate"}),
        indexes = @Index(name = "idx_visit_date_place", columnList = "visitedDate, placeId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false)
    private Long placeId;

    /** 실제 방문은 SeoulTime.today(). 샘플은 과거 날짜를 쓴다 */
    @Column(nullable = false)
    private LocalDate visitedDate;

    public static Visit create(String userId, Long placeId, LocalDate visitedDate) {
        Visit visit = new Visit();
        visit.userId = userId;
        visit.placeId = placeId;
        visit.visitedDate = visitedDate;
        return visit;
    }
}
