package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.formatMinutes;

import io.ludovicianul.timi.console.Ansi;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "stats",
    description =
        "Show aggregated time by tag or type for a day or date range (console formatted output)",
    mixinStandardHelpOptions = true)
public class StatsCommand implements Runnable {

  public enum GroupBy {
    type,
    tag
  }

  public enum CountMode {
    full,
    split
  }

  @Option(names = "--group-by", defaultValue = "type", description = "Group by 'type' or 'tag'")
  GroupBy groupBy;

  @Option(
      names = "--count-mode",
      defaultValue = "split",
      description = "Count mode: 'split' (default) or 'full'")
  CountMode countMode;

  @Option(names = "--day", description = "Stats for a single day (e.g. 2025-03-28)")
  LocalDate day;

  @Option(names = "--from", description = "Start date (inclusive)")
  LocalDate from;

  @Option(names = "--to", description = "End date (inclusive)")
  LocalDate to;

  @Option(
      names = "--daily-breakdown",
      description = "Show a per-day breakdown table alongside the summary")
  boolean showDailyBreakdown;

  @Option(
      names = "--weekly-breakdown",
      description = "Show a weekly breakdown table with trends compared to previous week")
  boolean showWeeklyBreakdown;

  @Inject EntryStore entryStore;
  @Inject Ansi ansi;

  @Override
  public void run() {
    if (day != null) {
      from = to = day;
    } else if (from != null && to != null) {
      if (from.isAfter(to)) {
        System.out.println("\n❌ Invalid date range: --from must be before or equal to --to.");
        return;
      }
    } else {
      from = to = LocalDate.now();
    }

    Predicate<LocalDate> dateFilter = d -> !d.isBefore(from) && !d.isAfter(to);

    List<TimeEntry> entries =
        entryStore.loadAllEntries(null).stream()
            .filter(e -> dateFilter.test(e.startTime().toLocalDate()))
            .toList();

    if (entries.isEmpty()) {
      System.out.printf("\n📭 No entries found from %s to %s.%n", from, to);
      return;
    }

    if (showWeeklyBreakdown && !from.equals(to)) {
      printWeeklyTrend(entries);
      return;
    }

    if (showDailyBreakdown && !from.equals(to)) {
      printDailyTrend(entries);
      return;
    }

    System.out.println(
        "\nℹ️ Use --daily-breakdown or --weekly-breakdown for detailed trend views.");
  }

  private void printWeeklyTrend(List<TimeEntry> entries) {
    WeekFields wf = WeekFields.ISO;
    var weekly =
        aggregateEntriesByDateKey(
            entries,
            e -> {
              LocalDate d = e.startTime().toLocalDate();
              return String.format("%d-W%02d", d.getYear(), d.get(wf.weekOfWeekBasedYear()));
            });
    printTrend("📈 Weekly Breakdown by " + groupBy, weekly, label -> String.format("%-10s", label));
  }

  private void printDailyTrend(List<TimeEntry> entries) {
    var daily = aggregateEntriesByDateKey(entries, e -> e.startTime().toLocalDate().toString());
    printTrend("📆 Daily Breakdown with Trends", daily, label -> String.format("%-12s", label));
  }

  private Map<String, Map<String, Integer>> aggregateEntriesByDateKey(
      List<TimeEntry> entries, Function<TimeEntry, String> keyFn) {
    Map<String, Map<String, Integer>> result = new TreeMap<>();

    for (TimeEntry e : entries) {
      String key = keyFn.apply(e);
      result.putIfAbsent(key, new TreeMap<>());
      Map<String, Integer> map = result.get(key);

      if (groupBy == GroupBy.type) {
        map.merge(e.activityType(), e.durationMinutes(), Integer::sum);
      } else {
        int share =
            (countMode == CountMode.split && !e.tags().isEmpty())
                ? e.durationMinutes() / e.tags().size()
                : e.durationMinutes();
        for (String tag : e.tags()) {
          map.merge(tag, share, Integer::sum);
        }
      }
    }

    return result;
  }

  private Set<String> computeAllGroups(Map<String, Map<String, Integer>> groupedData) {
    return groupedData.values().stream()
        .flatMap(m -> m.keySet().stream())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private Map<String, Integer> computeColumnWidths(
      Map<String, Map<String, Integer>> groupedData, Set<String> allGroups) {
    Map<String, Integer> widths = new HashMap<>();
    for (String group : allGroups) {
      int max =
          groupedData.values().stream()
              .map(m -> formatMinutes(m.getOrDefault(group, 0)).length())
              .max(Integer::compareTo)
              .orElse(5);
      widths.put(group, Math.max(max, group.length()) + 3);
    }
    return widths;
  }

  private void printHeader(String label, Set<String> allGroups, Map<String, Integer> widths) {
    System.out.printf("%s", label);
    for (String group : allGroups) {
      System.out.printf("  %" + widths.get(group) + "s", group);
    }
    System.out.printf("  %6s%n", "TOTAL");
  }

  private void printSeparator(int labelWidth, Set<String> allGroups, Map<String, Integer> widths) {
    System.out.print("-".repeat(labelWidth));
    for (String group : allGroups) {
      System.out.print("  " + "-".repeat(widths.get(group)));
    }
    System.out.print("  ------\n");
  }
  private void printTrend(
      String title,
      Map<String, Map<String, Integer>> groupedData,
      Function<String, String> labelFormatter) {

    System.out.println("\n" + title + "\n");

    Set<String> allGroups = computeAllGroups(groupedData);
    List<String> groupList = new ArrayList<>(allGroups);

    Map<String, Map<String, String>> formatted = new LinkedHashMap<>();
    Map<String, Integer> previousTotals = null;
    Map<String, Integer> previousGroupValues = new HashMap<>();

    // Precompute formatted cells
    for (var entry : groupedData.entrySet()) {
      String label = entry.getKey();
      Map<String, Integer> values = entry.getValue();
      Map<String, String> row = new LinkedHashMap<>();

      for (String group : groupList) {
        int curr = values.getOrDefault(group, 0);
        int prev = previousGroupValues.getOrDefault(group, 0);
        previousGroupValues.put(group, curr);

        String value = curr > 0
            ? formatMinutes(curr) + " " + trendArrow(curr, prev)
            : "-";
        row.put(group, value);
      }

      formatted.put(label, row);
    }

    // Compute actual column widths (stripped of ANSI)
    Map<String, Integer> columnWidths = new LinkedHashMap<>();
    for (String group : groupList) {
      int max = Math.max(group.length(),
          formatted.values().stream()
              .map(m -> stripAnsi(m.get(group)).length())
              .max(Integer::compareTo)
              .orElse(0)
      );
      columnWidths.put(group, max);
    }

    String labelHeader = labelFormatter.apply("LABEL");
    int labelWidth = labelHeader.length();

    // Header
    System.out.printf("%-" + labelWidth + "s", "LABEL");
    for (String group : groupList) {
      System.out.printf("  %" + columnWidths.get(group) + "s", group);
    }
    System.out.printf("  %6s%n", "TOTAL");

    // Separator
    System.out.print("-".repeat(labelWidth));
    for (String group : groupList) {
      System.out.print("  " + "-".repeat(columnWidths.get(group)));
    }
    System.out.print("  ------\n");

    // Rows
    for (var entry : formatted.entrySet()) {
      String label = entry.getKey();
      Map<String, String> row = entry.getValue();
      Map<String, Integer> raw = groupedData.get(label);

      int total = raw.values().stream().mapToInt(Integer::intValue).sum();
      int prevTotal = previousTotals == null ? 0 :
          previousTotals.values().stream().mapToInt(Integer::intValue).sum();

      System.out.printf("%-" + labelWidth + "s", label);

      for (String group : groupList) {
        String value = row.get(group);
        int pad = columnWidths.get(group) - stripAnsi(value).length();
        System.out.print("  " + " ".repeat(pad) + value);
      }

      String totalCell = formatMinutes(total);
      if (previousTotals != null) {
        totalCell += " " + trendArrow(total, prevTotal);
      }

      System.out.printf("  %s%n", totalCell);
      previousTotals = raw;
    }
  }

  private String trendArrow(int current, int previous) {
    if (current > previous) return ansi.green("▲");
    if (current < previous) return ansi.red("▼");
    return "-";
  }

  private static String stripAnsi(String input) {
    return input == null ? "" : input.replaceAll("\u001B\\[[;\\d]*m", "");
  }
}
