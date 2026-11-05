package com.hackathon.domain.place.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지하철역 (T Data 지하철역 좌표정보). 환승역은 호선마다 한 행이고,
 * 같은 역명이라도 호선별 위치가 다를 수 있어(신촌 2호선/경의중앙선) stationCode로 구분한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Station extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** T Data outStnNum (역 외부코드) */
    @Column(nullable = false, unique = true, length = 10)
    private String stationCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 30)
    private String line;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    public static Station of(String stationCode) {
        Station station = new Station();
        station.stationCode = stationCode;
        return station;
    }

    public void update(String name, String line, double latitude, double longitude) {
        this.name = name;
        this.line = line;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
