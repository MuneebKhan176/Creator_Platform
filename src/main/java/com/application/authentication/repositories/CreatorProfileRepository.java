package com.application.authentication.repositories;

import com.application.authentication.entities.CreatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, Long> {
}
