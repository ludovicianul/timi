package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.formatMinutes;

import io.ludovicianul.timi.console.Ansi;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    description = "List all time entries",
    mixinStandardHelpOptions = true)
public class ListCommand implements Runnable {

  enum CountMode {
    full,
    split
  }

  @CommandLine.Option(
      names = {"--month", "-m"},
      description = "Month filter (format: yyyy-MM)")
  String month;

  @CommandLine.Option(names = "--from", description = "Start date (format: yyyy-MM-dd)")
  LocalDate from;

  @CommandLine.Option(names = "--day", description = "Specific date (format: yyyy-MM-dd)")
  LocalDate day;

  @CommandLine.Option(names = "--to", description = "End date (format: yyyy-MM-dd)")
  LocalDate to;

  @CommandLine.Option(names = "--today", description = "List entries for today only")
  boolean today;

  @CommandLine.Option(names = "--yesterday", description = "List entries for yesterday only")
  boolean yesterday;

  @CommandLine.Option(
      names = "--only-tag",
      description = "Show only entries with this tag (case-insensitive)")
  String onlyTag;

  @CommandLine.Option(
      names = "--only-meta-tag",
      description = "Show only entries with this meta tag (case-insensitive)")
  String onlyMetaTag;

  @CommandLine.Option(names = "--show-tags", description = "Show tags for each entry")
  boolean showTags;

  @CommandLine.Option(names = "--show-meta-tags", description = "Show meta tags for each entry")
  boolean showMetaTags;

  @CommandLine.Option(
      names = "--show-ids",
      description = "Show entry IDs for edit/delete operations")
  boolean showIds;

  @CommandLine.Option(
      names = "--count-mode",
      description = "How to count time per tag: full (default: split)",
      defaultValue = "split")
  CountMode countMode;

  @Inject EntryStore entryStore;
  @Inject Ansi ansi;

  @Override
  public void run() {
    System.out.println(" ");
    List<TimeEntry> entries =
        entryStore.loadAllEntries(month).stream()
            .sorted(Comparator.comparing(TimeEntry::startTime))
            .filter(e -> filterByDateRange(e.startTime().toLocalDate()))
            .filter(e -> e.tagsMatching(onlyTag))
            .filter(e -> e.metaTagsMatching(onlyMetaTag))
            .toList();

    if (entries.isEmpty()) {
      System.out.println("📭 No entries found.");
      return;
    }

    LocalDate lastDate = null;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    int tagColumnWidth =
        entries.stream()
                .mapToInt(
                    e -> {
                      if (showTags && !e.tags().isEmpty()) {
                        return String.join(", ", e.tags()).length();
                      } else if (showMetaTags && !e.metaTags().isEmpty()) {
                        return String.join(", ", e.metaTags()).length();
                      } else {
                        return 0;
                      }
                    })
                .max()
                .orElse(0)
            + 2;

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
        System.out.printf(ansi.cyan("📅 %s (Total: %s)%n"), entryDate, totalFormatted);
        System.out.println("-".repeat(30));
        lastDate = entryDate;
      }

      String start = e.startTime().format(dtf);
      String duration =
          String.format("%dh %02dm", e.durationMinutes() / 60, e.durationMinutes() % 60);

      String tagText =
          showTags && !e.tags().isEmpty()
              ? String.join(", ", e.tags())
              : showMetaTags && !e.metaTags().isEmpty() ? String.join(", ", e.metaTags()) : "";

      if (showTags || showMetaTags) {
        System.out.printf("%-" + tagColumnWidth + "s", tagText + " • ");
      }

      if (showIds) {
        System.out.printf("%s • ", e.id());
      }

      System.out.printf("[%s]  %s  • %-12s  • %s%n", start, duration, e.activityType(), e.note());
    }

    if (showTags) {
      System.out.println();
      System.out.println("📎 Daily Tag Usage:");
      Map<LocalDate, Map<String, Integer>> dailyTagMap = new TreeMap<>();

      for (TimeEntry e : entries) {
        LocalDate date = e.startTime().toLocalDate();
        dailyTagMap.putIfAbsent(date, new HashMap<>());

        List<String> tags = new ArrayList<>(e.tags());
        int timePerTag =
            (countMode == CountMode.split && !tags.isEmpty())
                ? e.durationMinutes() / tags.size()
                : e.durationMinutes();

        for (String tag : tags) {
          dailyTagMap.get(date).merge(tag.toLowerCase(), timePerTag, Integer::sum);
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

    if (showTags) {
      System.out.println();
      System.out.printf("📦 Overall Tag Summary (%s mode):%n", countMode);
      Map<String, Integer> tagTotals = new TreeMap<>();

      for (TimeEntry e : entries) {
        List<String> tags = new ArrayList<>(e.tags());
        int timePerTag =
            (countMode == CountMode.split && !tags.isEmpty())
                ? e.durationMinutes() / tags.size()
                : e.durationMinutes();

        for (String tag : tags) {
          tagTotals.merge(tag.toLowerCase(), timePerTag, Integer::sum);
        }
      }

      tagTotals.entrySet().stream()
          .sorted(Map.Entry.comparingByValue())
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new))
          .forEach(
              (tag, minutes) -> System.out.printf("  • %-15s %s%n", tag, formatMinutes(minutes)));
    }
  }

  private boolean filterByDateRange(LocalDate entryDate) {
    if (from == null && to == null && day == null && !today && !yesterday) {
      return true;
    }

    if (today) {
      return entryDate.equals(LocalDate.now());
    }
    if (yesterday) {
      return entryDate.equals(LocalDate.now().minusDays(1));
    }

    if (day != null && !entryDate.equals(day)) {
      return false;
    }

    LocalDate fromDate = from != null ? from : LocalDate.MIN;
    LocalDate toDate = to != null ? to : LocalDate.MAX;
    return !entryDate.isBefore(fromDate) && !entryDate.isAfter(toDate);
  }
}
