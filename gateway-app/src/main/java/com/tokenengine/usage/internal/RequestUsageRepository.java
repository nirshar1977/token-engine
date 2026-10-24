package com.tokenengine.usage.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RequestUsageRepository extends JpaRepository<RequestUsageEntity, UUID> {
}
