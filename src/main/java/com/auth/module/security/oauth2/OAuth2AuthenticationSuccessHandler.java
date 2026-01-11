package com.auth.module.security.oauth2;

import com.auth.module.exception.BadRequestException;
import com.auth.module.model.RefreshToken;
import com.auth.module.security.JwtTokenProvider;
import com.auth.module.security.UserPrincipal;
import com.auth.module.service.RefreshTokenService;
import com.auth.module.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.auth.module.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("#{'${app.oauth2.authorized-redirect-uris}'.split(',')}")
    private List<String> authorizedRedirectUris;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info("OAuth2 authentication successful for user: {}",
            ((UserPrincipal) authentication.getPrincipal()).getEmail());

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.warn("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        logger.info("Redirecting OAuth2 authenticated user to: {}", targetUrl);
        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        logger.debug("Determining target URL for OAuth2 redirect");

        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        if (redirectUri.isPresent()) {
            logger.debug("Redirect URI from cookie: {}", redirectUri.get());
            if (!isAuthorizedRedirectUri(redirectUri.get())) {
                logger.error("Unauthorized redirect URI: {}", redirectUri.get());
                throw new BadRequestException("Sorry! We've got an Unauthorized Redirect URI and can't proceed with the authentication");
            }
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());
        logger.debug("Target URL for redirect: {}", targetUrl);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        logger.debug("Generating tokens for OAuth2 user: {}", userPrincipal.getEmail());

        String accessToken = tokenProvider.generateAccessToken(userPrincipal.getId(), userPrincipal.getEmail());

        // Create refresh token for OAuth2 login
        String deviceInfo = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                userPrincipal.getId(),
                deviceInfo,
                ipAddress
        );

        logger.info("OAuth2 tokens generated successfully for userId: {}", userPrincipal.getId());

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken.getToken())
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);

        return authorizedRedirectUris.stream()
                .anyMatch(authorizedRedirectUri -> {
                    URI authorizedURI = URI.create(authorizedRedirectUri);
                    return authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                            && authorizedURI.getPort() == clientRedirectUri.getPort();
                });
    }
}
