package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.formatMinutes;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import picocli.CommandLine.*;

@Command(
    name = "analyze",
    description = "Analyze time logs for patterns, context switching, and usage peaks",
    mixinStandardHelpOptions = true)
public class AnalyzeCommand implements Runnable {

  @Option(names = "--context-switch", description = "Show days/weeks with most context switching")
  boolean contextSwitch;

  @Option(names = "--by", description = "Grouping level: day or week", defaultValue = "day")
  SplitBy by;

  @Option(names = "--target", description = "Target tag or activity for peak analysis")
  String target;

  @Option(names = "--peak", description = "Analyze peak usage of a specific tag or activity")
  boolean peak;

  @Option(names = "--focus-score", description = "Analyze deep vs shallow work score")
  boolean focusScore;

  @Option(names = "--dow-insights", description = "Day-of-week usage summary")
  boolean dowInsights;

  @Inject EntryStore entryStore;

  enum SplitBy {
    day,
    week
  }

  @Override
  public void run() {
    List<TimeEntry> entries = entryStore.loadAllEntries(null);

    if (entries.isEmpty()) {
      System.out.println("📭 No entries found.");
      return;
    }

    if (contextSwitch) {
      analyzeContextSwitch(entries);
    }
    if (peak && target != null) {
      analyzePeakUsage(entries, target.toLowerCase());
    } else if (peak) {
      System.out.println("❌ Please provide a target for peak analysis.");
      return;
    }
    if (focusScore) {
      analyzeDeepVsShallow(entries);
    }
    if (dowInsights) {
      analyzeDayOfWeekInsights(entries);
    }
    if (!contextSwitch && !peak && !focusScore && !dowInsights) {
      summarize(entries);
      analyzeDeepVsShallow(entries);
    }
  }

  private void analyzeContextSwitch(List<TimeEntry> entries) {
    System.out.printf("\n📊 Context Switching Analysis by %s%n%n", by.name().toUpperCase());

    Map<String, Set<String>> grouped = new TreeMap<>();

    for (TimeEntry e : entries) {
      String key =
          by == SplitBy.week
              ? getWeekKey(e.startTime().toLocalDate())
              : e.startTime().toLocalDate().toString();

      grouped.putIfAbsent(key, new HashSet<>());
      grouped.get(key).addAll(e.tags().stream().map(String::toLowerCase).toList());
      grouped.get(key).add(e.activityType().toLowerCase());
    }

    grouped.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
        .limit(10)
        .forEach(
            entry -> {
              System.out.printf(
                  "%s → %d unique types/tags: %s%n",
                  entry.getKey(), entry.getValue().size(), String.join(", ", entry.getValue()));
            });
  }

  private void analyzePeakUsage(List<TimeEntry> entries, String target) {
    System.out.printf("\n📊 Peak Usage for '%s'%n%n", target);

    Map<String, Integer> grouped = new TreeMap<>();

    for (TimeEntry e : entries) {
      String key =
          by == SplitBy.week
              ? getWeekKey(e.startTime().toLocalDate())
              : e.startTime().toLocalDate().toString();

      boolean matches =
          e.tags().stream().anyMatch(t -> t.equalsIgnoreCase(target))
              || e.activityType().equalsIgnoreCase(target);

      if (matches) {
        grouped.merge(key, e.durationMinutes(), Integer::sum);
      }
    }

    grouped.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .limit(10)
        .forEach(
            e -> {
              System.out.printf("%s → %s%n", e.getKey(), formatMinutes(e.getValue()));
            });
  }

  private void summarize(List<TimeEntry> entries) {
    System.out.println("\n📈 Overall Summary\n");

    Set<String> allTags =
        entries.stream()
            .flatMap(e -> e.tags().stream())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

    Set<String> allTypes =
        entries.stream().map(e -> e.activityType().toLowerCase()).collect(Collectors.toSet());

    Map<LocalDate, List<TimeEntry>> byDay =
        entries.stream().collect(Collectors.groupingBy(e -> e.startTime().toLocalDate()));

    int totalMinutes = entries.stream().mapToInt(TimeEntry::durationMinutes).sum();
    double avgDaily = totalMinutes / (double) byDay.size();

    TimeEntry longestDay =
        entries.stream()
            .collect(
                Collectors.groupingBy(
                    e -> e.startTime().toLocalDate(),
                    Collectors.summingInt(TimeEntry::durationMinutes)))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(
                e ->
                    new TimeEntry(
                        null, e.getKey().atStartOfDay(), e.getValue(), null, "", Set.of()))
            .orElse(null);

    var mostCommonTag =
        entries.stream()
            .flatMap(e -> e.tags().stream())
            .collect(Collectors.groupingBy(String::toLowerCase, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue());

    var mostCommonType =
        entries.stream()
            .map(TimeEntry::activityType)
            .collect(Collectors.groupingBy(String::toLowerCase, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue());

    System.out.printf("• Unique Tags: %d%n", allTags.size());
    System.out.printf("• Unique Activity Types: %d%n", allTypes.size());
    System.out.printf("• Avg Daily Time: %s%n", formatMinutes((int) avgDaily));

    if (longestDay != null) {
      System.out.printf(
          "• Max Time in a Day: %s (%s)%n",
          formatMinutes(longestDay.durationMinutes()), longestDay.startTime().toLocalDate());
    }

    mostCommonTag.ifPresent(
        e -> System.out.printf("• Most Common Tag: %s (%d uses)%n", e.getKey(), e.getValue()));

    mostCommonType.ifPresent(
        e -> System.out.printf("• Most Common Activity: %s (%d uses)%n", e.getKey(), e.getValue()));
  }

  private void analyzeDeepVsShallow(List<TimeEntry> entries) {
    System.out.println("\n🧠 Deep vs Shallow Work Analysis\n");

    Map<LocalDate, List<TimeEntry>> byDay =
        entries.stream().collect(Collectors.groupingBy(e -> e.startTime().toLocalDate()));

    for (var entry : byDay.entrySet()) {
      LocalDate date = entry.getKey();
      List<TimeEntry> logs = entry.getValue();
      Set<String> uniqueTypes =
          logs.stream()
              .map(TimeEntry::activityType)
              .map(String::toLowerCase)
              .collect(Collectors.toSet());
      int total = logs.stream().mapToInt(TimeEntry::durationMinutes).sum();

      String profile =
          uniqueTypes.size() == 2
              ? "🔵 Deep Work"
              : uniqueTypes.size() <= 3 ? "🟡 Focused" : "🔴 Context Switching";
      System.out.printf("%s → %s (%s)%n", date, profile, formatMinutes(total));
    }
    System.out.println(
        "\n🔵 Deep Work = 2 types; 🟡 Focused = 3 types; 🔴 Context Switching > 3 types");
  }

  private void analyzeDayOfWeekInsights(List<TimeEntry> entries) {
    System.out.println("\n📆 Day of Week Insights\n");

    Map<DayOfWeek, Integer> dowTotals = new TreeMap<>();
    for (TimeEntry e : entries) {
      DayOfWeek dow = e.startTime().getDayOfWeek();
      dowTotals.merge(dow, e.durationMinutes(), Integer::sum);
    }

    dowTotals.forEach(
        (dow, minutes) -> {
          System.out.printf("%s → %s%n", dow, formatMinutes(minutes));
        });
  }

  private String getWeekKey(LocalDate date) {
    WeekFields weekFields = WeekFields.of(Locale.getDefault());
    int week = date.get(weekFields.weekOfWeekBasedYear());
    int year = date.getYear();
    return String.format("Week %02d, %d", week, year);
  }
}
