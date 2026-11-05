package com.hackathon.domain.scene.repository;

import com.hackathon.domain.scene.entity.Scene;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SceneRepository extends JpaRepository<Scene, Long> {

    Optional<Scene> findByPlaceIdAndNationalityAndLanguage(Long placeId, Nationality nationality, Language language);
}
