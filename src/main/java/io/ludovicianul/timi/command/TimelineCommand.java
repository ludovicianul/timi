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
    List<TimeEntry> entries = loadEntriesBetween(entryStore, from, to);
    if (entries.isEmpty()) {
      System.out.printf("📭 No entries between %s and %s.%n", from, to);
      return;
    }

    Map<String, Map<String, Integer>> aggregated = aggregate(entries);
    printStackedBarChart(aggregated);
  }

  private Map<String, Map<String, Integer>> aggregate(List<TimeEntry> entries) {
    WeekFields wf = WeekFields.ISO;
    Map<String, Map<String, Integer>> result = new TreeMap<>();
    for (TimeEntry e : entries) {
      if (!matchesOnlyFilter(e)) continue;

      String period =
          switch (viewMode) {
            case day -> e.startTime().toLocalDate().toString();
            case week -> {
              LocalDate d = e.startTime().toLocalDate();
              yield d.getYear() + "-W" + String.format("%02d", d.get(wf.weekOfWeekBasedYear()));
            }
            case month -> YearMonth.from(e.startTime()).toString();
          };

      result.putIfAbsent(period, new TreeMap<>());

      List<String> groups =
          switch (groupBy) {
            case type -> List.of(e.activityType());
            case tag -> new ArrayList<>(e.tags());
            case metaTag -> new ArrayList<>(e.metaTags());
          };

      int share = groups.isEmpty() ? e.durationMinutes() : e.durationMinutes() / groups.size();

      for (String g : groups) {
        result.get(period).merge(g, share, Integer::sum);
      }
    }
    return result;
  }

  private boolean matchesOnlyFilter(TimeEntry entry) {
    if (only == null || only.isBlank()) return true;
    return switch (groupBy) {
      case type -> entry.activityType().equalsIgnoreCase(only);
      case tag -> entry.tags().stream().anyMatch(t -> t.equalsIgnoreCase(only));
      case metaTag -> entry.metaTags().stream().anyMatch(t -> t.equalsIgnoreCase(only));
    };
  }

  private void printStackedBarChart(Map<String, Map<String, Integer>> data) {
    Set<String> keys =
        data.values().stream()
            .flatMap(m -> m.keySet().stream())
            .collect(Collectors.toCollection(TreeSet::new));

    List<Integer> colorCodes =
        List.of(
            160, 33, 118, 220, 141, 39, 203, 75, 208, 45, 93, 99, 129, 69, 214, 186, 123, 105, 33,
            27, 36, 84, 124, 203, 229);
    Map<String, String> colorMap = new HashMap<>();
    int colorIndex = 0;

    for (String key : keys) {
      int code = colorCodes.get(colorIndex % colorCodes.size());
      colorMap.put(key, ansi.color256("█", code));
      colorIndex++;
    }

    System.out.println("\n📊 Aggregated Timeline (" + viewMode + ")");
    if (only != null && !only.isBlank()) {
      System.out.printf("🔎 Focus: %s\n", ansi.cyan(only));
    }
    System.out.println("=".repeat(80));

    int maxMinutes =
        data.values().stream().flatMap(m -> m.values().stream()).max(Integer::compareTo).orElse(1);

    for (var entry : data.entrySet()) {
      String period = entry.getKey();
      Map<String, Integer> values = entry.getValue();
      int total = values.values().stream().mapToInt(Integer::intValue).sum();

      StringBuilder bar = new StringBuilder();
      for (var k : values.entrySet()) {
        int segment = scale(k.getValue(), maxMinutes);
        bar.append(colorMap.get(k.getKey()).repeat(segment));
      }

      System.out.printf("%-15s | %s (%s)%n", period, bar.toString(), formatMinutes(total));
    }

    System.out.println("=".repeat(80));

    if (only == null || only.isBlank()) {
      System.out.println("Legend:");
      for (String k : keys) {
        System.out.printf("  %s %s%n", colorMap.get(k), k);
      }
    }
  }

  private int scale(int value, int maxValue) {
    return Math.max(1, (int) Math.ceil((value / (double) maxValue) * chartWidth));
  }
}
