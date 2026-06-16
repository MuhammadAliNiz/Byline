package me.alinizamani.byline.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.alinizamani.byline.domain.user.User;
import me.alinizamani.byline.domain.user.UserProfile;
import me.alinizamani.byline.exception.ConflictException;
import me.alinizamani.byline.exception.FileProcessingException;
import me.alinizamani.byline.exception.ResourceNotFoundException;
import me.alinizamani.byline.exception.UnprocessableEntityException;
import me.alinizamani.byline.repository.UserFollowRepository;
import me.alinizamani.byline.repository.UserProfileRepository;
import me.alinizamani.byline.service.S3Service;
import me.alinizamani.byline.service.UserService;
import me.alinizamani.byline.util.FileValidator;
import me.alinizamani.byline.web.dto.request.ChangeUsernameRequest;
import me.alinizamani.byline.web.dto.request.UpdateProfileRequest;
import me.alinizamani.byline.web.dto.response.ChangeUsernameResponse;
import me.alinizamani.byline.web.dto.response.MeResponse;
import me.alinizamani.byline.web.dto.response.PublicProfileResponse;
import me.alinizamani.byline.web.mapper.UserProfileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserProfileRepository userProfileRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserProfileMapper userProfileMapper;

    private final S3Service s3Service;
    private final FileValidator fileValidator;

    @Value("${app.backend-base-url}")
    String backendUrl;

    @Override
    public MeResponse getMe(User user) {
        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));


        return new MeResponse(
                user.getId(),
                userProfile.getUsername(),
                user.getEmail(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getBio(),
                userProfile.getAvatarUrl(),
                userProfile.getWebsiteUrl(),
                userProfile.getTwitterHandle(),
                userProfile.getLinkedinUrl(),
                userProfile.getGithubUrl(),
                user.getRole(),
                user.isEmailVerified(),
                userProfile.getFollowersCount(),
                userProfile.getFollowingCount(),
                userProfile.getArticlesCount(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Override
    public PublicProfileResponse getUserByUsername(User user, String username) {
        UserProfile userProfile = userProfileRepository.findByUsername(username).orElseThrow(
                () -> new ResourceNotFoundException("User profile not found")
        );

        boolean isFollowing = false;

        if (user != null) {
            isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(
                    user.getId(),
                    userProfile.getUser().getId()
            );
        }

        return new PublicProfileResponse(
                userProfile.getUser().getId(),
                userProfile.getUsername(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getBio(),
                userProfile.getAvatarUrl(),
                userProfile.getWebsiteUrl(),
                userProfile.getTwitterHandle(),
                userProfile.getLinkedinUrl(),
                userProfile.getGithubUrl(),
                userProfile.getFollowersCount(),
                userProfile.getFollowingCount(),
                userProfile.getArticlesCount(),
                isFollowing,
                userProfile.getCreatedAt()
        );
    }

    @Transactional
    @Override
    public MeResponse updateProfile(User user, UpdateProfileRequest updateProfileRequest) {
        UserProfile userProfile = userProfileRepository.findByUserId(user.getId()).orElseThrow(
                () -> new ResourceNotFoundException("User profile not found")
        );

        userProfileMapper.updateProfileFromRequest(updateProfileRequest, userProfile);

        return getMe(user);
    }

    @Transactional
    @Override
    public ChangeUsernameResponse changeUsername(User user, ChangeUsernameRequest changeUsernameRequest) {
        if(userProfileRepository.existsByUsername(changeUsernameRequest.newUsername())){
            throw new ConflictException("Username is already taken");
        }

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId()).orElseThrow(
                () -> new ResourceNotFoundException("User profile not found")
        );

        Instant allowedChangeDate = userProfile.getUsernameUpdatedAt().plus(30, ChronoUnit.DAYS);

        if (allowedChangeDate.isAfter(Instant.now())) {
            throw new UnprocessableEntityException("You can only change your username once every 30 days.");
        }

        String oldUsername = userProfile.getUsername();

        userProfile.setUsername(changeUsernameRequest.newUsername());
        userProfile.setUsernameUpdatedAt(Instant.now());
        userProfileRepository.save(userProfile);

        return new ChangeUsernameResponse(user.getId(), oldUsername, changeUsernameRequest.newUsername(), userProfile.getUpdatedAt());
    }

    @Transactional
    @Override
    public String uploadAvatar(User user, MultipartFile avatarFile) {
        String mimeType = fileValidator.validateImage(avatarFile);

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId()).orElseThrow(
                () -> new ResourceNotFoundException("User profile not found")
        );

        if (userProfile.getAvatarUrl() != null && !userProfile.getAvatarUrl().isEmpty()) {
            String avatarKey = userProfile.getAvatarUrl().replace("/api/public/avatar/", "");
            try {
                s3Service.deleteAvatar(avatarKey);
            }catch (Exception e){
                //noinspection LoggingSimilarMessage
                log.warn("Failed to delete avatar from S3 for user {}: {}", user.getId(), e.getMessage());
            }
        }

        String avatarKey;
        try {
            avatarKey = s3Service.uploadAvatar(avatarFile, mimeType);
        } catch (Exception e) {
            log.error("Error uploading avatar for user {}: {}", user.getId(), e.getMessage());
            throw new FileProcessingException("Failed to upload avatar : " + e.getMessage());
        }

        userProfile.setAvatarUrl("/api/public/avatar/" + avatarKey);
        userProfile.setAvatarMimeType(mimeType);
        userProfileRepository.save(userProfile);

        return backendUrl + userProfile.getAvatarUrl();
    }

    @Transactional
    @Override
    public void deleteAvatar(User user) {
        UserProfile userProfile = userProfileRepository.findByUserId(user.getId()).orElseThrow(
                () -> new ResourceNotFoundException("User profile not found")
        );

        if (userProfile.getAvatarUrl() != null && !userProfile.getAvatarUrl().isEmpty()) {
            String oldAvatarKey = userProfile.getAvatarUrl().replace("/api/public/avatar/", "");
            try {
                s3Service.deleteAvatar(oldAvatarKey);
            }catch (Exception e){
                log.warn("Failed to delete avatar from S3 for user {}: {}", user.getId(), e.getMessage());
            }
        }

        userProfile.setAvatarUrl(null);
        userProfile.setAvatarMimeType(null);
        userProfileRepository.save(userProfile);
    }
}
