package me.alinizamani.byline.repository;

import me.alinizamani.byline.domain.user.PasswordResetToken;
import me.alinizamani.byline.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    void deleteAllByUser(User user);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
