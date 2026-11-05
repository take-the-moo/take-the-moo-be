package com.hackathon.domain.user.repository;

import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.global.common.Nationality;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    /** 국적별 샘플 사용자. 만든 순 → id 순 (같은 시각에 만들어도 순서가 늘 같다) */
    List<AppUser> findAllBySampleTrueAndNationalityOrderByCreatedAtAscIdAsc(Nationality nationality);

    List<AppUser> findAllByIdInAndSampleTrue(Collection<String> ids);

    List<AppUser> findAllBySampleTrue();
}
