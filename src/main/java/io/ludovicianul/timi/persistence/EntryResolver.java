package io.ludovicianul.timi.persistence;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class EntryResolver {
  @Inject EntryStore entryStore;

  public Optional<TimeEntry> resolveEntry(String id) {
    UUID entryId;
    try {
      entryId = UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      System.out.println("❌ Invalid ID format.");
      return Optional.empty();
    }

    Optional<TimeEntry> entryOpt = entryStore.findById(entryId);
    if (entryOpt.isEmpty()) {
      System.out.println("Entry not found.");
    }

    return entryOpt;
  }
}
