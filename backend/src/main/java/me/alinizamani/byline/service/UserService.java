package me.alinizamani.byline.service;

import jakarta.validation.Valid;
import me.alinizamani.byline.domain.user.User;
import me.alinizamani.byline.web.dto.request.ChangeUsernameRequest;
import me.alinizamani.byline.web.dto.request.UpdateProfileRequest;
import me.alinizamani.byline.web.dto.response.ChangeUsernameResponse;
import me.alinizamani.byline.web.dto.response.MeResponse;
import me.alinizamani.byline.web.dto.response.PublicProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.attribute.UserPrincipal;

public interface UserService {

    MeResponse getMe(User user);

    PublicProfileResponse getUserByUsername(User user, String username);

    MeResponse updateProfile(User user, UpdateProfileRequest request);

    ChangeUsernameResponse changeUsername(User user, @Valid ChangeUsernameRequest changeUsernameRequest);

    String uploadAvatar(User user, MultipartFile avatarFile);

    void deleteAvatar(User user);
}
