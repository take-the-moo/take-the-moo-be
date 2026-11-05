package com.hackathon.domain.review.repository;

import com.hackathon.domain.review.entity.ReviewTranslation;
import com.hackathon.global.common.Language;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewTranslationRepository extends JpaRepository<ReviewTranslation, Long> {

    List<ReviewTranslation> findAllByReviewIdInAndLanguage(Collection<Long> reviewIds, Language language);
}
