package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    description = "List all time entries",
    mixinStandardHelpOptions = true)
public class ListCommand implements Runnable {

  @CommandLine.Option(
      names = {"--month", "-m"},
      description = "Month filter (format: yyyy-MM)")
  String month;

  @CommandLine.Option(names = "--from", description = "Start date (format: yyyy-MM-dd)")
  String from;

  @CommandLine.Option(names = "--to", description = "End date (format: yyyy-MM-dd)")
  String to;

  @CommandLine.Option(
      names = "--only-tag",
      description = "Show only entries with this tag (case-insensitive)")
  String onlyTag;

  @CommandLine.Option(names = "--show-tags", description = "Show tags for each entry")
  boolean showTags;

  @CommandLine.Option(
      names = "--show-ids",
      description = "Show entry IDs for edit/delete operations")
  boolean showIds;

  @Inject EntryStore entryStore;

  @Override
  public void run() {
    List<TimeEntry> entries =
        entryStore.loadAllEntries(month).stream()
            .sorted(Comparator.comparing(TimeEntry::startTime))
            .filter(e -> filterByDateRange(e.startTime().toLocalDate()))
            .filter(
                e ->
                    onlyTag == null || e.tags().stream().anyMatch(t -> t.equalsIgnoreCase(onlyTag)))
            .toList();

    if (entries.isEmpty()) {
      System.out.println("📭 No entries found.");
      return;
    }

    LocalDate lastDate = null;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    for (TimeEntry e : entries) {
      LocalDate entryDate = e.startTime().toLocalDate();

      if (!entryDate.equals(lastDate)) {
        if (lastDate != null) {
          System.out.println();
        }

        int totalMinutes =
            entries.stream()
                .filter(x -> x.startTime().toLocalDate().equals(entryDate))
                .mapToInt(TimeEntry::durationMinutes)
                .sum();

        String totalFormatted = String.format("%dh %02dm", totalMinutes / 60, totalMinutes % 60);
        System.out.printf("📅 %s (Total: %s)%n", entryDate, totalFormatted);
        System.out.println("-".repeat(60));
        lastDate = entryDate;
      }

      String start = e.startTime().format(dtf);
      String duration =
          String.format("%dh %02dm", e.durationMinutes() / 60, e.durationMinutes() % 60);

      if (showIds) {
        System.out.printf("%s • ", e.id());
      }

      System.out.printf("[%s]  %s  • %-12s  • %s", start, duration, e.activityType(), e.note());
      if (showTags && !e.tags().isEmpty()) {
        System.out.print("  • Tags: " + String.join(", ", e.tags()));
      }
      System.out.println();
    }

    // Daily tag summary
    if (showTags) {
      System.out.println();
      System.out.println("📎 Daily Tag Usage:");
      Map<LocalDate, Map<String, Integer>> dailyTagMap = new TreeMap<>();
      for (TimeEntry e : entries) {
        LocalDate date = e.startTime().toLocalDate();
        dailyTagMap.putIfAbsent(date, new HashMap<>());
        for (String tag : e.tags()) {
          dailyTagMap.get(date).merge(tag.toLowerCase(), e.durationMinutes(), Integer::sum);
        }
      }

      for (var dayEntry : dailyTagMap.entrySet()) {
        System.out.printf("  %s: ", dayEntry.getKey());
        String summary =
            dayEntry.getValue().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> String.format("%s (%s)", e.getKey(), formatMinutes(e.getValue())))
                .collect(Collectors.joining(", "));
        System.out.println(summary);
      }
    }

    // Overall tag summary
    if (showTags) {
      System.out.println();
      System.out.println("📦 Overall Tag Summary:");
      Map<String, Integer> tagTotals = new TreeMap<>();
      for (TimeEntry e : entries) {
        for (String tag : e.tags()) {
          tagTotals.merge(tag.toLowerCase(), e.durationMinutes(), Integer::sum);
        }
      }

      tagTotals.forEach(
          (tag, minutes) -> System.out.printf("  • %-15s %s%n", tag, formatMinutes(minutes)));
    }
  }

  private boolean filterByDateRange(LocalDate entryDate) {
    if (from == null && to == null) return true;

    try {
      LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.MIN;
      LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.MAX;
      return !entryDate.isBefore(fromDate) && !entryDate.isAfter(toDate);
    } catch (DateTimeParseException e) {
      System.out.println("❌ Invalid date format. Use yyyy-MM-dd for --from and --to.");
      return false;
    }
  }

  private String formatMinutes(int minutes) {
    return String.format("%dh %02dm", minutes / 60, minutes % 60);
  }
}
