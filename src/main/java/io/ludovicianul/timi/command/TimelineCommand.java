package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.*;

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
      names = "--chart-width",
      defaultValue = "30",
      description = "Width of the bar chart (number of characters)")
  int chartWidth;

  @Inject EntryStore entryStore;

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
        .collect(Collectors.toList());
  }

  private Map<String, Integer> aggregateByDay(List<TimeEntry> entries) {
    return entries.stream()
        .collect(
            Collectors.groupingBy(
                e -> e.startTime().toLocalDate().toString(),
                TreeMap::new,
                Collectors.summingInt(TimeEntry::durationMinutes)));
  }

  private Map<String, Integer> aggregateByWeek(List<TimeEntry> entries) {
    WeekFields wf = WeekFields.ISO;
    return entries.stream()
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
        .collect(
            Collectors.groupingBy(
                e -> YearMonth.from(e.startTime()).toString(),
                TreeMap::new,
                Collectors.summingInt(TimeEntry::durationMinutes)));
  }

  private void printBarChart(Map<String, Integer> aggregatedData) {
    int maxMinutes = aggregatedData.values().stream().max(Integer::compareTo).orElse(1);
    System.out.println("\n📊 Aggregated Timeline (" + viewMode + ")");
    System.out.println("=".repeat(80));

    aggregatedData.forEach(
        (period, minutes) -> {
          String bar = "█".repeat(scale(minutes, maxMinutes));
          System.out.printf(
              "%-15s | %-" + chartWidth + "s (%s)%n", period, bar, formatMinutes(minutes));
        });

    System.out.println("=".repeat(80));
  }

  private int scale(int value, int maxValue) {
    return (int) Math.ceil((value / (double) maxValue) * chartWidth);
  }
}
