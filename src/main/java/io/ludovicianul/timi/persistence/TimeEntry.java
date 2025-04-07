package io.ludovicianul.timi.persistence;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
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
    @JsonSetter(nulls = Nulls.AS_EMPTY) Set<String> tags,
    @JsonSetter(nulls = Nulls.AS_EMPTY) Set<String> metaTags) {

  public boolean tagsMatching(String tag) {
    return tag == null || tags.stream().anyMatch(t -> t.equalsIgnoreCase(tag));
  }

  public boolean metaTagsMatching(String metaTag) {
    return metaTag == null || metaTags.stream().anyMatch(t -> t.equalsIgnoreCase(metaTag));
  }
}
