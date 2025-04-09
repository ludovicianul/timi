package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.*;

import io.ludovicianul.timi.console.Ansi;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "timeline",
    description =
        "Show aggregated timeline (day/week/month) between dates. Example: timeline --from 2025-01-01 --to 2025-01-31 --view day",
    mixinStandardHelpOptions = true)
public class TimelineCommand implements Runnable {

  enum ViewMode {
    day,
    week,
    month
  }

  enum GroupBy {
    tag,
    metaTag,
    type
  }

  @CommandLine.Option(names = "--from", required = true, description = "Start date (yyyy-MM-dd)")
  LocalDate from;

  @CommandLine.Option(names = "--to", required = true, description = "End date (yyyy-MM-dd)")
  LocalDate to;

  @CommandLine.Option(
      names = "--view",
      defaultValue = "day",
      description = "Aggregation view (day/week/month)")
  ViewMode viewMode;

  @CommandLine.Option(
      names = "--group-by",
      defaultValue = "type",
      description = "Group by 'tag', 'metaTag', or 'type'")
  GroupBy groupBy;

  @CommandLine.Option(
      names = "--only",
      description = "Only show chart for the specified tag/type/metaTag")
  String only;

  @CommandLine.Option(
      names = "--chart-width",
      defaultValue = "30",
      description = "Width of the bar chart (number of characters)")
  int chartWidth;

  @Inject EntryStore entryStore;
  @Inject Ansi ansi;

  @Override
  public void run() {
    List<TimeEntry> entries = loadEntriesBetween(from, to);
    if (entries.isEmpty()) {
      System.out.printf("📭 No entries between %s and %s.%n", from, to);
      return;
    }

    Map<String, Integer> aggregated =
        switch (viewMode) {
          case day -> aggregateByDay(entries);
          case week -> aggregateByWeek(entries);
          case month -> aggregateByMonth(entries);
        };

    printBarChart(aggregated);
  }

  private List<TimeEntry> loadEntriesBetween(LocalDate from, LocalDate to) {
    Set<String> months = new HashSet<>();
    LocalDate cursor = from.withDayOfMonth(1);
    while (!cursor.isAfter(to)) {
      months.add(cursor.toString().substring(0, 7));
      cursor = cursor.plusMonths(1);
    }
    return months.stream()
        .flatMap(m -> entryStore.loadAllEntries(m).stream())
        .filter(
            e -> {
              LocalDate d = e.startTime().toLocalDate();
              return !d.isBefore(from) && !d.isAfter(to);
            })
        .toList();
  }

  private Map<String, Integer> aggregateByDay(List<TimeEntry> entries) {
    return entries.stream()
        .filter(this::matchesOnlyFilter)
        .collect(
            Collectors.groupingBy(
                e -> e.startTime().toLocalDate().toString(),
                TreeMap::new,
                Collectors.summingInt(TimeEntry::durationMinutes)));
  }

  private Map<String, Integer> aggregateByWeek(List<TimeEntry> entries) {
    WeekFields wf = WeekFields.ISO;
    return entries.stream()
        .filter(this::matchesOnlyFilter)
        .collect(
            Collectors.groupingBy(
                e -> {
                  LocalDate d = e.startTime().toLocalDate();
                  return d.getYear()
                      + "-W"
                      + String.format("%02d", d.get(wf.weekOfWeekBasedYear()));
                },
                TreeMap::new,
                Collectors.summingInt(TimeEntry::durationMinutes)));
  }

  private Map<String, Integer> aggregateByMonth(List<TimeEntry> entries) {
    return entries.stream()
        .filter(this::matchesOnlyFilter)
        .collect(
            Collectors.groupingBy(
                e -> YearMonth.from(e.startTime()).toString(),
                TreeMap::new,
                Collectors.summingInt(TimeEntry::durationMinutes)));
  }

  private boolean matchesOnlyFilter(TimeEntry entry) {
    if (only == null || only.isBlank()) return true;
    return switch (groupBy) {
      case type -> entry.activityType().equalsIgnoreCase(only);
      case tag -> entry.tags().stream().anyMatch(t -> t.equalsIgnoreCase(only));
      case metaTag -> entry.metaTags().stream().anyMatch(t -> t.equalsIgnoreCase(only));
    };
  }

  private void printBarChart(Map<String, Integer> aggregatedData) {
    int maxMinutes = aggregatedData.values().stream().max(Integer::compareTo).orElse(1);
    System.out.println("\n📊 Aggregated Timeline (" + viewMode + ")");
    if (only != null && !only.isBlank()) {
      System.out.printf("🔎 Focus: %s\n", ansi.cyan(only));
    }
    System.out.println("=".repeat(80));

    aggregatedData.forEach(
        (period, minutes) -> {
          String bar = "█".repeat(scale(minutes, maxMinutes));
          String label = (only != null && !only.isBlank()) ? ansi.green(bar) : bar;
          System.out.printf("%-15s | %s (%s)%n", period, label, formatMinutes(minutes));
        });

    System.out.println("=".repeat(80));
  }

  private int scale(int value, int maxValue) {
    return (int) Math.ceil((value / (double) maxValue) * chartWidth);
  }
}
