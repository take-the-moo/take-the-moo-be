package com.hackathon.domain.user.repository;

import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.global.common.Nationality;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findFirstBySampleTrueAndNationalityOrderByCreatedAtAsc(Nationality nationality);

    List<AppUser> findAllByIdInAndSampleTrue(Collection<String> ids);
}
