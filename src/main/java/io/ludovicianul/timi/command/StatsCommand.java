package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "stats",
    description = "Show aggregated time by tag or type for a day or date range",
    mixinStandardHelpOptions = true)
public class StatsCommand implements Runnable {

  @Option(names = "--group-by", defaultValue = "type", description = "Group by 'type' or 'tag'")
  GroupBy groupBy;

  @Option(names = "--day", description = "Stats for a single day (e.g. 2025-03-28)")
  String day;

  @Option(names = "--from", description = "Start date (inclusive)")
  String from;

  @Option(names = "--to", description = "End date (inclusive)")
  String to;

  @Option(
      names = "--daily-breakdown",
      description = "Show a per-day breakdown table alongside the summary")
  boolean showDailyBreakdown;

  private LocalDate fromDate;
  private LocalDate toDate;

  @Inject EntryStore entryStore;

  @Override
  public void run() {
    // Parse dates
    if (day != null) {
      fromDate = toDate = parseDate(day, "--day");
    } else if (from != null && to != null) {
      fromDate = parseDate(from, "--from");
      toDate = parseDate(to, "--to");
      if (fromDate.isAfter(toDate)) {
        System.out.println("❌ Invalid date range: --from must be before or equal to --to.");
        return;
      }
    } else {
      fromDate = toDate = LocalDate.now();
    }

    Predicate<LocalDate> dateFilter = d -> !d.isBefore(fromDate) && !d.isAfter(toDate);

    List<TimeEntry> entries =
        entryStore.loadAllEntries(null).stream()
            .filter(e -> dateFilter.test(e.startTime().toLocalDate()))
            .toList();

    if (entries.isEmpty()) {
      System.out.printf("📭 No entries found from %s to %s.%n", fromDate, toDate);
      return;
    }

    // Aggregate totals
    Map<String, Integer> totals = new TreeMap<>();
    for (TimeEntry e : entries) {
      if (GroupBy.type == groupBy) {
        totals.merge(e.activityType(), e.durationMinutes(), Integer::sum);
      } else if (GroupBy.tag == groupBy) {
        for (String tag : e.tags()) {
          totals.merge(tag, e.durationMinutes(), Integer::sum);
        }
      }
    }

    // Print totals
    int totalMinutes = totals.values().stream().mapToInt(Integer::intValue).sum();
    int maxLabelLength = totals.keySet().stream().mapToInt(String::length).max().orElse(12);
    String format = "  %-" + (maxLabelLength + 2) + "s %8s  %6s%n";

    String range = fromDate.equals(toDate) ? fromDate.toString() : fromDate + " → " + toDate;

    System.out.printf("📊 Time Summary by %s for %s%n%n", groupBy, range);
    System.out.printf(format, "Group", "Time", "Share");
    int totalWidth = String.format(format, "", "", "").length();
    System.out.println("-".repeat(totalWidth - 1));

    for (var entry : totals.entrySet()) {
      int minutes = entry.getValue();
      String time = formatMinutes(minutes);
      double share = (minutes * 100.0) / totalMinutes;
      System.out.printf(format, entry.getKey(), time, String.format("%.1f%%", share));
    }

    System.out.printf("%n🕒 Total Time: %s%n", formatMinutes(totalMinutes));

    // Per-day breakdown table
    if (showDailyBreakdown && !fromDate.equals(toDate)) {
      System.out.printf("%n📆 Daily Breakdown (%s to %s):%n%n", fromDate, toDate);

      Map<LocalDate, Map<String, Integer>> byDay = new TreeMap<>();
      for (TimeEntry e : entries) {
        LocalDate date = e.startTime().toLocalDate();
        byDay.putIfAbsent(date, new TreeMap<>());
        if (GroupBy.type == groupBy) {
          byDay.get(date).merge(e.activityType(), e.durationMinutes(), Integer::sum);
        } else {
          for (String tag : e.tags()) {
            byDay.get(date).merge(tag, e.durationMinutes(), Integer::sum);
          }
        }
      }

      // Get all group keys across all days
      Set<String> allGroups =
          byDay.values().stream()
              .flatMap(map -> map.keySet().stream())
              .collect(Collectors.toCollection(TreeSet::new));

      int groupWidth = allGroups.stream().mapToInt(String::length).max().orElse(10) + 2;

      // Header row
      StringBuilder header = new StringBuilder();
      header.append(String.format("%-12s", "Date"));
      for (String group : allGroups) {
        header.append(String.format("  %-" + groupWidth + "s", group));
      }
      System.out.println(header);

      // Separator line
      System.out.println("-".repeat(header.length()));

      // Data rows
      for (var dayEntry : byDay.entrySet()) {
        System.out.printf("%-12s", dayEntry.getKey());
        for (String group : allGroups) {
          int mins = dayEntry.getValue().getOrDefault(group, 0);
          System.out.printf("  %-" + groupWidth + "s", mins > 0 ? formatMinutes(mins) : "-");
        }
        System.out.println();
      }
    }
  }

  private LocalDate parseDate(String input, String label) {
    try {
      return LocalDate.parse(input);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("❌ Invalid format for " + label + ": use yyyy-MM-dd");
    }
  }

  private String formatMinutes(int minutes) {
    return String.format("%dh %02dm", minutes / 60, minutes % 60);
  }

  public enum GroupBy {
    type,
    tag
  }
}
