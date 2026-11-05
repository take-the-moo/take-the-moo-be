package com.hackathon.domain.place.repository;

import com.hackathon.domain.place.entity.ExplanationTranslation;
import com.hackathon.global.common.Language;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExplanationTranslationRepository extends JpaRepository<ExplanationTranslation, Long> {

    Optional<ExplanationTranslation> findByPlaceIdAndLanguage(Long placeId, Language language);
}
