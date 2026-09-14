package com.application.profile.entities;

import com.application.authentication.entities.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "creator_profiles")
public class CreatorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    // Storage key (e.g. "profile-pictures/<uuid>.jpg") kept separately from the
    // public URL so the old file can be deleted from R2 when it's replaced.
    @Column(name = "profile_picture_key", length = 255)
    private String profilePictureKey;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "banner_key", length = 255)
    private String bannerKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public CreatorProfile() {
    }

    public CreatorProfile(User user, String displayName, String bio) {
        this.user = user;
        this.displayName = displayName;
        this.bio = bio;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getProfilePictureKey() {
        return profilePictureKey;
    }

    public void setProfilePictureKey(String profilePictureKey) {
        this.profilePictureKey = profilePictureKey;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getBannerKey() {
        return bannerKey;
    }

    public void setBannerKey(String bannerKey) {
        this.bannerKey = bannerKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}