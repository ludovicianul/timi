package io.ludovicianul.timi.persistence.session;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record Session(
    UUID id,
    LocalDateTime start,
    boolean paused,
    LocalDateTime pausedAt,
    long totalPausedSeconds,
    String type,
    Set<String> tags,
    String note) {}
