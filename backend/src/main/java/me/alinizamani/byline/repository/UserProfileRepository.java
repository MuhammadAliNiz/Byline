package me.alinizamani.byline.repository;

import me.alinizamani.byline.domain.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findByUserId(UUID userId);
    boolean existsByUsername(String username);
}