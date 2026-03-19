package com.java.domain.provider;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.java.config.BadRequestException;
import com.java.domain.service.dto.GoogleUserInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Component
public class GoogleOAuthClient {

    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    public GoogleOAuthClient(
            @Value("${app.oauth.google.client-id}") String clientId,
            @Value("${app.oauth.google.client-secret}") String clientSecret
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.builder().build();
    }

    public GoogleUserInfo exchangeAndFetchUser(String authorizationCode, String redirectUri) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new BadRequestException("Missing authorization code");
        }

        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BadRequestException("Missing redirect URI");
        }

        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    authorizationCode,
                    redirectUri
            ).execute();

            String accessToken = tokenResponse.getAccessToken();
            if (accessToken == null || accessToken.isBlank()) {
                throw new BadRequestException("Failed to get access token from Google");
            }

            Map<?, ?> response = restClient.get()
                    .uri(USER_INFO_URL)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new BadRequestException("Failed to get user information from Google");
            }

            String email = asString(response.get("email"));
            String name = asString(response.get("name"));
            String picture = asString(response.get("picture"));
            String id = asString(response.get("id"));

            if (email == null || email.isBlank()) {
                throw new BadRequestException("Google did not return user email");
            }

            return new GoogleUserInfo(email, name, picture, id);
        } catch (IOException ex) {
            throw new BadRequestException("Google authentication failed: " + ex.getMessage());
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}