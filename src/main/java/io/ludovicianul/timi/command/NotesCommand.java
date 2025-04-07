package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "notes",
    description = "Show notes for a tag, filtered by day or month",
    mixinStandardHelpOptions = true)
public class NotesCommand implements Runnable {

  @Option(names = "--tag", description = "Tag to filter by (case-insensitive)")
  String tag;

  @Option(names = "--meta-tag", description = "Meta tag to filter by (case-insensitive)")
  String metaTag;

  @Option(names = "--month", description = "Month filter (format: yyyy-MM)")
  String month;

  @Option(names = "--day", description = "Day filter (format: yyyy-MM-dd)")
  String day;

  @Inject EntryStore entryStore;

  @Override
  public void run() {
    if (tag == null && metaTag == null) {
      System.out.println("\n❌ Please provide at least one of: --tag or --meta-tag");
      return;
    }
    Predicate<LocalDate> dateFilter = buildDateFilter();

    if (dateFilter == null) {
      System.out.println("\n❌ Invalid date format. Use yyyy-MM-dd for day or yyyy-MM for month.");
      return;
    }

    List<TimeEntry> entries =
        entryStore.loadAllEntries(null).stream()
            .filter(e -> e.tagsMatching(tag))
            .filter(e -> e.metaTagsMatching(metaTag))
            .filter(e -> dateFilter.test(e.startTime().toLocalDate()))
            .sorted(Comparator.comparing(TimeEntry::startTime))
            .toList();

    if (entries.isEmpty()) {
      System.out.printf(
          "\n📭 No notes found for tag '%s', meta tag %s%s%n",
          tag, metaTag, (day != null ? " on " + day : (month != null ? " in " + month : "")));
      return;
    }

    System.out.printf(
        "\n📝 Notes for tag '%s', meta tag %s%s:%n%n",
        tag, metaTag, (day != null ? " on " + day : (month != null ? " in " + month : "")));

    for (TimeEntry e : entries) {
      String time = e.startTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
      String duration =
          String.format("%dh %02dm", e.durationMinutes() / 60, e.durationMinutes() % 60);
      System.out.printf("[%s]  %s  • %s%n", time, duration, e.note());
    }
  }

  private Predicate<LocalDate> buildDateFilter() {
    try {
      if (day != null) {
        LocalDate target = LocalDate.parse(day);
        return d -> d.equals(target);
      } else if (month != null) {
        YearMonth target = YearMonth.parse(month);
        return d -> YearMonth.from(d).equals(target);
      }
    } catch (DateTimeParseException e) {
      return null;
    }

    return d -> true;
  }
}
