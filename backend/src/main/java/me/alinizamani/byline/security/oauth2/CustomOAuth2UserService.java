package me.alinizamani.byline.security.oauth2;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.alinizamani.byline.domain.user.*;
import me.alinizamani.byline.repository.UserProfileRepository;
import me.alinizamani.byline.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository        userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        log.info(oAuth2User.getAttributes().toString());

        String googleId  = oAuth2User.getAttribute("sub");
        String email     = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName  = oAuth2User.getAttribute("family_name");
        String avatar    = oAuth2User.getAttribute("picture");

        if (email == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Email not returned by Google"
            );
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> updateExistingUser(existing, googleId, avatar))
                .orElseGet(() -> createNewUser(email, googleId, firstName, lastName, avatar));

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private User updateExistingUser(User user, String googleId, String avatar) {

        if (user.getAuthProvider() == AuthProvider.LOCAL) {

            if (!user.isEmailVerified()) {

                log.info("Converting unverified local account to Google account for email: {}", user.getEmail());
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setEmailVerified(true);
            } else {

                throw new OAuth2AuthenticationException(
                        new OAuth2Error("email_already_registered"),
                        "An account with this email already exists. Please log in with your password."
                );
            }
        }

        user.setGoogleId(googleId);
        userRepository.save(user);

        userProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            profile.setAvatarUrl(avatar);
            userProfileRepository.save(profile);
        });

        return user;
    }

    private User createNewUser(String email, String googleId,
                               String firstName, String lastName, String avatar) {

        User user = new User();
        user.setEmail(email);
        user.setGoogleId(googleId);
        user.setEmailVerified(true);      // Google already verified it
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setAvatarUrl(avatar);
        profile.setUsername(resolveUsername(firstName, email));
        userProfileRepository.save(profile);

        log.info("New user registered via Google: {}", email);
        return user;
    }

    private String resolveUsername(String firstName, String email) {
        String base = (firstName != null ? firstName : email.split("@")[0])
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");

        if (base.isBlank()) base = "user";

        if (!userProfileRepository.existsByUsername(base)) return base;

        int suffix = 1;
        while (userProfileRepository.existsByUsername(base + suffix)) suffix++;
        return base + suffix;
    }
}