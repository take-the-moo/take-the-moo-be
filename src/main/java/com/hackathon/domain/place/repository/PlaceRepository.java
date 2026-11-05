package com.hackathon.domain.place.repository;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByContentId(String contentId);

    List<Place> findAllByStatus(PlaceStatus status);

    List<Place> findAllByContentIdIn(Collection<String> contentIds);
}
