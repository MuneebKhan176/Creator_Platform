package com.application.profile.controllers;

import com.application.authentication.security.UserPrincipal;
import com.application.profile.dtos.ApiResponse;
import com.application.profile.dtos.ProfileResponse;
import com.application.profile.dtos.UpdateProfileRequest;
import com.application.profile.services.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/*
 * Namespaced under /api/user/** on purpose (not /api/creator/**): every
 * verified user gets a CreatorProfile automatically (see AuthService), but
 * only carries the base USER role unless promoted. SecurityConfig already
 * permits USER/CREATOR/BUSINESS/ADMIN/MODERATOR on /api/user/**, so this
 * needed no SecurityConfig changes.
 */
@RestController
@RequestMapping("/api/user/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileResponse profile = profileService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse profile = profileService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    @PostMapping(value = "/picture", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfilePicture(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        ProfileResponse profile = profileService.updateProfilePicture(principal.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated", profile));
    }

    @PostMapping(value = "/banner", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateBanner(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        ProfileResponse profile = profileService.updateBanner(principal.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Banner updated", profile));
    }
}