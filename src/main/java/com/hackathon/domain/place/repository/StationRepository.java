package com.hackathon.domain.place.repository;

import com.hackathon.domain.place.entity.Station;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByStationCode(String stationCode);

    List<Station> findAllByName(String name);
}
