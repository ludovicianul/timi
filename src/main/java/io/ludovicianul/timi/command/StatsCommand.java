package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.formatMinutes;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.*;
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

  @Inject EntryStore entryStore;

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

    Map<String, Integer> totals = new TreeMap<>();
    Map<String, Set<String>> tagActivityMap = new HashMap<>();
    Map<String, Integer> tagEntryCount = new HashMap<>();

    for (TimeEntry e : entries) {
      if (groupBy == GroupBy.type) {
        totals.merge(e.activityType(), e.durationMinutes(), Integer::sum);
      } else if (groupBy == GroupBy.tag) {
        List<String> tags = new ArrayList<>(e.tags());
        int share =
            (countMode == CountMode.split && !tags.isEmpty())
                ? e.durationMinutes() / tags.size()
                : e.durationMinutes();
        for (String tag : tags) {
          totals.merge(tag, share, Integer::sum);
          tagActivityMap.computeIfAbsent(tag, t -> new TreeSet<>()).add(e.activityType());
          tagEntryCount.merge(tag, 1, Integer::sum);
        }
      }
    }

    int totalMinutes = totals.values().stream().mapToInt(Integer::intValue).sum();
    String range = from.equals(to) ? from.toString() : from + " → " + to;

    System.out.printf("\n📊 Time Summary by %s for %s%n", groupBy, range);
    if (groupBy == GroupBy.tag) {
      System.out.printf(
          "💡 Count Mode: %s (entry time %s tags)%n",
          countMode, countMode == CountMode.split ? "distributed across" : "fully assigned to");
    }
    System.out.println();

    // Print the summary table with the simplified format
    printSimplifiedSummaryTable(totals, totalMinutes, tagActivityMap, tagEntryCount);

    System.out.printf("\n🕒 Total Time: %s%n", formatMinutes(totalMinutes));

    if (showDailyBreakdown && !from.equals(to)) {
      System.out.printf("\n📆 Daily Breakdown (%s to %s):%n%n", from, to);

      Map<LocalDate, Map<String, Integer>> byDay = new TreeMap<>();
      for (TimeEntry e : entries) {
        LocalDate date = e.startTime().toLocalDate();
        byDay.putIfAbsent(date, new TreeMap<>());

        if (groupBy == GroupBy.type) {
          byDay.get(date).merge(e.activityType(), e.durationMinutes(), Integer::sum);
        } else {
          List<String> tags = new ArrayList<>(e.tags());
          int share =
              (countMode == CountMode.split && !tags.isEmpty())
                  ? e.durationMinutes() / tags.size()
                  : e.durationMinutes();
          for (String tag : tags) {
            byDay.get(date).merge(tag, share, Integer::sum);
          }
        }
      }

      Set<String> allGroups =
          byDay.values().stream()
              .flatMap(m -> m.keySet().stream())
              .collect(Collectors.toCollection(TreeSet::new));

      // Print the daily breakdown with the simplified format
      printSimplifiedBreakdownTable(byDay, allGroups);
    }
  }

  private void printSimplifiedSummaryTable(
      Map<String, Integer> totals,
      int totalMinutes,
      Map<String, Set<String>> tagActivityMap,
      Map<String, Integer> tagEntryCount) {

    // Calculate column widths
    int groupWidth =
        Math.max(10, totals.keySet().stream().mapToInt(String::length).max().orElse(5));
    int timeWidth = 10;
    int shareWidth = 8;
    int typesWidth = 30;
    int entriesWidth = 7;

    // Print header with simple format
    String headerFormat = "%-" + groupWidth + "s  %-" + timeWidth + "s  %-" + shareWidth + "s";
    if (groupBy == GroupBy.tag) {
      headerFormat += "  %-" + typesWidth + "s  %-" + entriesWidth + "s";
      System.out.printf(headerFormat, "GROUP", "TIME", "SHARE", "TYPES", "ENTRIES");
    } else {
      System.out.printf(headerFormat, "GROUP", "TIME", "SHARE");
    }
    System.out.println();

    // Print separator
    String separatorFormat = "%" + groupWidth + "s  %" + timeWidth + "s  %" + shareWidth + "s";
    if (groupBy == GroupBy.tag) {
      separatorFormat += "  %" + typesWidth + "s  %" + entriesWidth + "s";
      System.out.printf(
          separatorFormat,
          "-".repeat(groupWidth),
          "-".repeat(timeWidth),
          "-".repeat(shareWidth),
          "-".repeat(typesWidth),
          "-".repeat(entriesWidth));
    } else {
      System.out.printf(
          separatorFormat, "-".repeat(groupWidth), "-".repeat(timeWidth), "-".repeat(shareWidth));
    }
    System.out.println();

    // Print data rows
    String rowFormat = "%-" + groupWidth + "s  %-" + timeWidth + "s  %" + shareWidth + ".1f%%";
    String tagRowFormat = rowFormat + "  %-" + typesWidth + "s  %-" + entriesWidth + "s";

    for (var entry : totals.entrySet()) {
      int minutes = entry.getValue();
      String time = formatMinutes(minutes);
      double share = (minutes * 100.0) / totalMinutes;

      if (groupBy == GroupBy.tag) {
        String types =
            formatTypes(tagActivityMap.getOrDefault(entry.getKey(), Set.of()), typesWidth);
        String count = String.valueOf(tagEntryCount.getOrDefault(entry.getKey(), 0));
        System.out.printf(tagRowFormat, entry.getKey(), time, share, types, count);
      } else {
        System.out.printf(rowFormat, entry.getKey(), time, share);
      }
      System.out.println();
    }
  }

  private String formatTypes(Set<String> types, int maxWidth) {
    String joined = String.join(", ", types);
    if (joined.length() > maxWidth - 3) {
      return joined.substring(0, maxWidth - 3) + "...";
    }
    return joined;
  }

  private void printSimplifiedBreakdownTable(
      Map<LocalDate, Map<String, Integer>> byDay, Set<String> allGroups) {
    List<String> groupsList = new ArrayList<>(allGroups);

    // Calculate column widths
    int dateWidth = 10; // YYYY-MM-DD is 10 chars
    Map<String, Integer> columnWidths = new HashMap<>();
    for (String group : groupsList) {
      int width = Math.max(group.length(), 8); // At least 8 chars for time format
      columnWidths.put(group, width);
    }

    // Print header row
    System.out.printf("%-" + dateWidth + "s", "DATE");
    for (String group : groupsList) {
      System.out.printf("  %-" + columnWidths.get(group) + "s", group);
    }
    System.out.println();

    // Print separator
    System.out.print("-".repeat(dateWidth));
    for (String group : groupsList) {
      System.out.print("  " + "-".repeat(columnWidths.get(group)));
    }
    System.out.println();

    // Print data rows
    for (var dayEntry : byDay.entrySet()) {
      System.out.printf("%-" + dateWidth + "s", dayEntry.getKey());

      for (String group : groupsList) {
        int mins = dayEntry.getValue().getOrDefault(group, 0);
        String value = mins > 0 ? formatMinutes(mins) : "-";
        System.out.printf("  %-" + columnWidths.get(group) + "s", value);
      }
      System.out.println();
    }
  }
}
