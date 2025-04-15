package io.ludovicianul.timi.persistence;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

@RegisterForReflection
public record UndoAction(
    String action, LocalDateTime timestamp, TimeEntry entryBefore, TimeEntry entryAfter) {}
