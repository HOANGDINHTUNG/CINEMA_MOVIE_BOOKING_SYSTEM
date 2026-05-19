package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserUserId(Long userId);
}
