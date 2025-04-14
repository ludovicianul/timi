package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.formatMinutes;

import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import io.ludovicianul.timi.util.Utils;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import picocli.CommandLine.*;

@Command(
    name = "audit",
    description = "Audit entries for common issues",
    mixinStandardHelpOptions = true)
public class AuditCommand implements Runnable {

  @Option(names = "--from", required = true, description = "Start date (yyyy-MM-dd)")
  LocalDate from;

  @Option(names = "--to", required = true, description = "End date (yyyy-MM-dd)")
  LocalDate to;

  @Option(names = "--empty-notes", description = "Flag entries with empty or missing notes")
  boolean checkEmptyNotes = true;

  @Option(
      names = "--tag-type-overuse",
      description = "Flag if a single tag or type appears in most entries")
  boolean checkTagTypeOveruse = true;

  @Option(names = "--suspicious-combos", description = "Flag unusual tag-type combinations")
  boolean checkSuspiciousCombos = true;

  @Option(
      names = "--odd-durations",
      description = "Flag durations that are oddly specific (not multiples of 5 or 10)")
  boolean checkOddDurations = true;

  @Option(names = "--summary", description = "Show only summary counts")
  boolean summaryOnly;

  @Inject EntryStore entryStore;
  @Inject ConfigManager configManager;

  @Override
  public void run() {
    List<TimeEntry> entries = Utils.loadEntriesBetween(entryStore, from, to);

    if (entries.isEmpty()) {
      System.out.printf("📭 No entries found from %s to %s.%n", from, to);
      return;
    }

    System.out.printf("\n📋 Audit Report from %s to %s%n", from, to);
    System.out.println("=".repeat(50));

    int flaggedShort = 0;
    int flaggedEmptyNotes = 0;
    int flaggedOddDurations = 0;

    Map<String, Long> tagFreq = new HashMap<>();
    Map<String, Long> typeFreq = new HashMap<>();
    Map<String, Set<String>> tagToTypes = new HashMap<>();

    for (TimeEntry e : entries) {
      if (e.durationMinutes() < configManager.getShortDurationThreshold()) {
        flaggedShort++;
      }
      if (checkEmptyNotes && (e.note() == null || e.note().trim().isEmpty())) {
        flaggedEmptyNotes++;
      }
      if (checkOddDurations && isOddDuration(e)) {
        flaggedOddDurations++;
      }

      for (String tag : e.tags()) {
        tagFreq.merge(tag.toLowerCase(), 1L, Long::sum);
        tagToTypes.computeIfAbsent(tag.toLowerCase(), k -> new HashSet<>()).add(e.activityType());
      }
      typeFreq.merge(e.activityType().toLowerCase(), 1L, Long::sum);
    }

    long total = entries.size();

    System.out.printf("• Entries analyzed: %d%n", total);
    System.out.printf(
        "• Short durations (<%dm): %d%n", configManager.getShortDurationThreshold(), flaggedShort);
    System.out.printf("• Empty notes: %d%n", flaggedEmptyNotes);
    System.out.printf("• Odd durations: %d%n", flaggedOddDurations);

    if (checkTagTypeOveruse) {
      tagFreq.entrySet().stream()
          .filter(e -> e.getValue() >= total * 0.8)
          .forEach(
              e ->
                  System.out.printf(
                      "⚠️ Tag overuse: '%s' used in %d entries%n", e.getKey(), e.getValue()));

      typeFreq.entrySet().stream()
          .filter(e -> e.getValue() >= total * 0.8)
          .forEach(
              e ->
                  System.out.printf(
                      "⚠️ Type overuse: '%s' used in %d entries%n", e.getKey(), e.getValue()));
    }

    if (checkSuspiciousCombos) {
      System.out.println("\n🔎 Suspicious Tag-Type Combinations:");
      tagToTypes.forEach(
          (tag, types) -> {
            if (types.size() == 1) {
              String type = types.iterator().next();
              System.out.printf("⚠️ Tag '%s' only used with type '%s'%n", tag, type);
            }
          });
    }

    if (!summaryOnly) {
      System.out.println("\n📄 Detailed Issues:");
      boolean found = false;

      for (TimeEntry e : entries) {
        if (e.durationMinutes() < configManager.getShortDurationThreshold()
            || (checkEmptyNotes && (e.note() == null || e.note().trim().isEmpty()))
            || (checkOddDurations && isOddDuration(e))) {
          System.out.printf(
              "%s [%s] %s (%s) → %s%n",
              e.id(),
              e.startTime(),
              formatMinutes(e.durationMinutes()),
              e.activityType(),
              (e.note() == null || e.note().isBlank()) ? "<empty note>" : e.note());
          found = true;
        }
      }
      if (!found) {
        System.out.println("No issues found in the detailed report.");
      }
    }
  }

  private static boolean isOddDuration(TimeEntry e) {
    return e.durationMinutes() % 10 != 0 && e.durationMinutes() % 10 != 5;
  }
}
