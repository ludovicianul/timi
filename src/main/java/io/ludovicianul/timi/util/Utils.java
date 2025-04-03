package io.ludovicianul.timi.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Utils {

  public static final DateTimeFormatter DEFAULT_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
    DateTimeFormatter[] formatters = {
      DateTimeFormatter.ISO_LOCAL_DATE_TIME,
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };
    for (DateTimeFormatter formatter : formatters) {
      try {
        return LocalDateTime.parse(dateTimeStr.trim(), formatter);
      } catch (DateTimeParseException e) {
        // try next
      }
    }
    throw new DateTimeParseException("Invalid date format", dateTimeStr, 0);
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
}
