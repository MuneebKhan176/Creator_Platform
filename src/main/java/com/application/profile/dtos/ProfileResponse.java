package com.application.profile.dtos;

import com.application.profile.entities.CreatorProfile;

import java.time.Instant;

public class ProfileResponse {

    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String location;
    private String websiteUrl;
    private String profilePictureUrl;
    private String bannerUrl;
    private Instant createdAt;

    public static ProfileResponse from(CreatorProfile profile) {
        ProfileResponse response = new ProfileResponse();
        response.username = profile.getUser().getUsername();
        response.email = profile.getUser().getEmail();
        response.displayName = profile.getDisplayName();
        response.bio = profile.getBio();
        response.location = profile.getLocation();
        response.websiteUrl = profile.getWebsiteUrl();
        response.profilePictureUrl = profile.getProfilePictureUrl();
        response.bannerUrl = profile.getBannerUrl();
        response.createdAt = profile.getCreatedAt();
        return response;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public String getLocation() {
        return location;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}