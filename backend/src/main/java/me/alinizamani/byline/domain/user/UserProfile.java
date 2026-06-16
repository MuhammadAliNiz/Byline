package me.alinizamani.byline.domain.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "user_profiles")
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    Instant usernameUpdatedAt = Instant.now();

    private String firstName;

    private String lastName;

    private String avatarUrl;

    private String avatarMimeType;

    @Column(length = 300)
    private String bio;

    private String websiteUrl;

    private String twitterHandle;

    private String linkedinUrl;

    private String githubUrl;

    @Column(nullable = false)

    private long followersCount = 0;

    @Column(nullable = false)
    private long followingCount = 0;

    @Column(nullable = false)
    private long articlesCount = 0;

    private Instant lastActivityAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
