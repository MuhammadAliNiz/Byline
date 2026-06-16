package me.alinizamani.byline.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.alinizamani.byline.domain.user.User;
import me.alinizamani.byline.service.UserService;
import me.alinizamani.byline.web.dto.request.ChangeUsernameRequest;
import me.alinizamani.byline.web.dto.request.UpdateProfileRequest;
import me.alinizamani.byline.web.dto.response.ApiResponse;
import me.alinizamani.byline.web.dto.response.ChangeUsernameResponse;
import me.alinizamani.byline.web.dto.response.MeResponse;
import me.alinizamani.byline.web.dto.response.PublicProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal User user) {
        MeResponse meResponse = userService.getMe(user);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(meResponse));
    }

    @GetMapping("/public/{username}")
    public ResponseEntity<ApiResponse<PublicProfileResponse>> getUserByUsername(
            @AuthenticationPrincipal User user,
            @PathVariable String username) {
        PublicProfileResponse publicProfileResponse = userService.getUserByUsername(user, username);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(publicProfileResponse));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request
    ){
        MeResponse updatedProfile = userService.updateProfile(user, request);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(updatedProfile)
        );
    }

    @PutMapping("/me/username")
    public ResponseEntity<ApiResponse<ChangeUsernameResponse>> updateUsername(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangeUsernameRequest changeUsernameRequest
    ){
        ChangeUsernameResponse changeUsernameResponse = userService.changeUsername(user, changeUsernameRequest);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(changeUsernameResponse));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @AuthenticationPrincipal User user,
                @RequestParam("avatar") MultipartFile avatarFile
    ){
        String avatarUrl = userService.uploadAvatar(user, avatarFile);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(Map.of("avatarUrl", avatarUrl)));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(
            @AuthenticationPrincipal User user
    ){
        userService.deleteAvatar(user);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Avatar removed. Default avatar restored."));
    }
}
