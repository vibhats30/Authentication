package com.auth.module.security.oauth2;

import com.auth.module.exception.OAuth2AuthenticationProcessingException;
import com.auth.module.model.AuthProvider;
import com.auth.module.model.User;
import com.auth.module.repository.UserRepository;
import com.auth.module.security.UserPrincipal;
import com.auth.module.security.oauth2.user.OAuth2UserInfo;
import com.auth.module.security.oauth2.user.OAuth2UserInfoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (OAuth2AuthenticationException ex) {
            // Re-throw OAuth2 exceptions as-is
            throw ex;
        } catch (Exception ex) {
            // Log the full exception for debugging
            logger.error("Error processing OAuth2 user: {}", ex.getMessage(), ex);
            // Provide a meaningful error message
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Error processing OAuth2 user: " + ex.getClass().getSimpleName();
            throw new OAuth2AuthenticationException(errorMessage);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                oAuth2UserRequest.getClientRegistration().getRegistrationId(),
                oAuth2User.getAttributes()
        );

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().equals(AuthProvider.valueOf(
                    oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))) {
                throw new OAuth2AuthenticationProcessingException(
                        "Looks like you're signed up with " + user.getProvider() +
                        " account. Please use your " + user.getProvider() + " account to login."
                );
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        String imageUrl = oAuth2UserInfo.getImageUrl();
        logger.debug("Registering new user with image URL length: {}", imageUrl != null ? imageUrl.length() : 0);
        logger.debug("Image URL: {}", imageUrl);
        logger.debug("Provider ID: {}", oAuth2UserInfo.getId());
        logger.debug("Provider ID length: {}", oAuth2UserInfo.getId() != null ? oAuth2UserInfo.getId().length() : 0);
        logger.debug("Name: {}", oAuth2UserInfo.getName());
        logger.debug("Email: {}", oAuth2UserInfo.getEmail());

        User user = User.builder()
                .provider(AuthProvider.valueOf(
                        oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))
                .providerId(oAuth2UserInfo.getId())
                .name(oAuth2UserInfo.getName())
                .email(oAuth2UserInfo.getEmail())
                .imageUrl(imageUrl)
                .emailVerified(true)
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setImageUrl(oAuth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }
}
