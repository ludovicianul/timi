package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.formatMinutes;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "search",
    description = "Search entries by tag, activity type, or note (fuzzy match)",
    mixinStandardHelpOptions = true)
public class SearchCommand implements Runnable {

  @Option(names = "--tag", description = "Search by tag (case-insensitive, match whole tag)")
  String tag;

  @Option(
      names = "--meta-tag",
      description = "Search by meta tag (case-insensitive, match whole tag)")
  String metaTag;

  @Option(
      names = "--type",
      description = "Search by activity type (case-insensitive, match whole type)")
  String activity;

  @Option(names = "--note", description = "Search by note content (case-insensitive, substring)")
  String note;

  @Option(names = "--from", description = "Start date filter (yyyy-MM-dd)")
  String from;

  @Option(names = "--to", description = "End date filter (yyyy-MM-dd)")
  String to;

  @Option(names = "--summary", description = "Show only total time and match count")
  boolean summaryOnly;

  private LocalDate fromDate;
  private LocalDate toDate;

  @Inject EntryStore entryStore;

  @Override
  public void run() {
    if (tag == null && activity == null && note == null && metaTag == null) {
      System.out.println("\n❌ Please provide at least one of: --tag, --metaTag, --type, or --note");
      return;
    }

    if ((from != null && to == null) || (from == null && to != null)) {
      System.out.println("\n❌ Please provide both --from and --to for date filtering.");
      return;
    }

    if (from != null) {
      fromDate = parseDate(from, "--from");
      toDate = parseDate(to, "--to");
      if (fromDate.isAfter(toDate)) {
        System.out.println("\n❌ Invalid date range: --from must be before or equal to --to.");
        return;
      }
    }

    List<TimeEntry> matches =
        entryStore.loadAllEntries(null).stream()
            .filter(
                e -> {
                  boolean tagMatch = e.tagsMatching(tag);
                  boolean metaTagMatch = e.metaTagsMatching(metaTag);
                  boolean activityMatch =
                      activity == null || e.activityType().equalsIgnoreCase(activity);
                  boolean noteMatch =
                      note == null
                          || (e.note() != null
                              && e.note().toLowerCase().contains(note.toLowerCase(Locale.ROOT)));
                  boolean dateMatch =
                      (fromDate == null
                          || (!e.startTime().toLocalDate().isBefore(fromDate)
                              && !e.startTime().toLocalDate().isAfter(toDate)));
                  return tagMatch && metaTagMatch && activityMatch && noteMatch && dateMatch;
                })
            .sorted(Comparator.comparing(TimeEntry::startTime))
            .toList();

    if (matches.isEmpty()) {
      System.out.println("\n📭 No entries matched your search.");
      return;
    }

    int totalMinutes = matches.stream().mapToInt(TimeEntry::durationMinutes).sum();

    if (summaryOnly) {
      System.out.printf(
          "\n📊 Total time for matching entries: %s (%d entr%s)%n",
          formatMinutes(totalMinutes), matches.size(), matches.size() == 1 ? "y" : "ies");
      return;
    }

    System.out.printf(
        "\n🔎 Found %d matching entr%s:%n%n", matches.size(), matches.size() == 1 ? "y" : "ies");

    for (TimeEntry e : matches) {
      String time = e.startTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
      String duration = formatMinutes(e.durationMinutes());
      System.out.printf(
          "[%s]  %s  • %-12s  • %s • %s • %s%n",
          time, duration, e.activityType(), e.tags(), e.metaTags(), e.note());
    }

    System.out.printf("%n🕒 Total Time: %s%n", formatMinutes(totalMinutes));
  }

  private LocalDate parseDate(String value, String label) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("❌ Invalid date for " + label + ". Use yyyy-MM-dd.");
    }
  }
}
