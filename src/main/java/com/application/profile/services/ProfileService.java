package com.application.profile.services;

import com.application.authentication.exceptions.ApiException;
import com.application.profile.dtos.ProfileResponse;
import com.application.profile.dtos.UpdateProfileRequest;
import com.application.profile.entities.CreatorProfile;
import com.application.profile.repositories.CreatorProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Service
public class ProfileService {

    private final CreatorProfileRepository creatorProfileRepository;
    private final CloudStorageService cloudStorageService;

    public ProfileService(CreatorProfileRepository creatorProfileRepository,
                           CloudStorageService cloudStorageService) {
        this.creatorProfileRepository = creatorProfileRepository;
        this.cloudStorageService = cloudStorageService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return ProfileResponse.from(getOrThrow(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        CreatorProfile profile = getOrThrow(userId);

        if (request.getDisplayName() != null) {
            String displayName = request.getDisplayName().trim();
            if (displayName.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Display name cannot be empty");
            }
            profile.setDisplayName(displayName);
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio().trim());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation().trim());
        }
        if (request.getWebsiteUrl() != null) {
            profile.setWebsiteUrl(request.getWebsiteUrl().trim());
        }

        profile.setUpdatedAt(Instant.now());
        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse updateProfilePicture(Long userId, MultipartFile file) {
        CreatorProfile profile = getOrThrow(userId);
        CloudStorageService.UploadedFile uploaded = cloudStorageService.upload(file, "profile-pictures");

        String oldKey = profile.getProfilePictureKey();
        profile.setProfilePictureUrl(uploaded.url());
        profile.setProfilePictureKey(uploaded.key());
        profile.setUpdatedAt(Instant.now());

        if (oldKey != null && !oldKey.isBlank()) {
            cloudStorageService.delete(oldKey);
        }

        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse updateBanner(Long userId, MultipartFile file) {
        CreatorProfile profile = getOrThrow(userId);
        CloudStorageService.UploadedFile uploaded = cloudStorageService.upload(file, "banners");

        String oldKey = profile.getBannerKey();
        profile.setBannerUrl(uploaded.url());
        profile.setBannerKey(uploaded.key());
        profile.setUpdatedAt(Instant.now());

        if (oldKey != null && !oldKey.isBlank()) {
            cloudStorageService.delete(oldKey);
        }

        return ProfileResponse.from(profile);
    }

    private CreatorProfile getOrThrow(Long userId) {
        return creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));
    }
}