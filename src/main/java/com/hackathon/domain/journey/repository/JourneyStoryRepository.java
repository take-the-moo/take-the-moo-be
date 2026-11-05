package com.hackathon.domain.journey.repository;

import com.hackathon.domain.journey.entity.JourneyStory;
import com.hackathon.global.common.Language;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JourneyStoryRepository extends JpaRepository<JourneyStory, Long> {

    Optional<JourneyStory> findByUserIdAndLanguage(String userId, Language language);
}
