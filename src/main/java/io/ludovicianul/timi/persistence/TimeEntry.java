package io.ludovicianul.timi.persistence;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RegisterForReflection
public record TimeEntry(
    UUID id,
    LocalDateTime startTime,
    int durationMinutes,
    String note,
    String activityType,
    Set<String> tags) {}
