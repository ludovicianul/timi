package io.ludovicianul.timi.util;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

  /**
   * Parses a date time string using several supported formats.
   *
   * @param dateTimeStr the input string
   * @return the parsed LocalDateTime
   * @throws DateTimeParseException if none of the formats match
   */
  public static LocalDateTime parseDateTime(String dateTimeStr) throws DateTimeParseException {
    if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
      return LocalDateTime.now();
    }

    String input = dateTimeStr.trim();

    DateTimeFormatter[] fullDateTimeFormatters = {
      DateTimeFormatter.ISO_LOCAL_DATE_TIME,
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };

    for (DateTimeFormatter formatter : fullDateTimeFormatters) {
      try {
        return LocalDateTime.parse(input, formatter);
      } catch (DateTimeParseException e) {
        // try next
      }
    }
    try {
      LocalTime time = LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm"));
      return LocalDateTime.of(LocalDate.now(), time);
    } catch (DateTimeParseException e) {
      throw new DateTimeParseException("Invalid date/time format", input, 0);
    }
  }

  /**
   * Formats a duration in minutes as "Xh YYm".
   *
   * @param minutes duration in minutes
   * @return formatted string
   */
  public static String formatMinutes(int minutes) {
    return String.format("%dh %02dm", minutes / 60, minutes % 60);
  }

  /**
   * Loads all entries between two dates from the entry store.
   *
   * @param entryStore the entry store to load from
   * @param from the start date (inclusive)
   * @param to the end date (inclusive)
   * @return a list of TimeEntry objects that fall within the date range
   */
  public static List<TimeEntry> loadEntriesBetween(
      EntryStore entryStore, LocalDate from, LocalDate to) {
    Set<String> months = new HashSet<>();
    LocalDate cursor = from.withDayOfMonth(1);
    while (!cursor.isAfter(to)) {
      months.add(cursor.toString().substring(0, 7)); // yyyy-MM
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
}
