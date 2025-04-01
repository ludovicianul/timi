package io.ludovicianul.timi.command;

import io.ludovicianul.timi.git.GitManager;
import io.ludovicianul.timi.persistence.EntryResolver;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "edit",
    description =
        "Edit an existing entry by ID. Specify fields to update via options, or use --interactive.",
    mixinStandardHelpOptions = true)
public class EditCommand implements Runnable {

  @Inject GitManager gitManager;
  @Inject EntryStore entryStore;
  @Inject EntryResolver entryResolver;

  @CommandLine.Option(
      names = {"--id", "-i"},
      description = "ID of the entry to edit.",
      required = true)
  private String id;

  @CommandLine.Option(
      names = {"--start", "-s"},
      description = "New start time (e.g., 'yyyy-MM-ddTHH:mm:ss' or 'yyyy-MM-dd HH:mm').")
  private String startTimeStr;

  @CommandLine.Option(
      names = {"--duration", "-d"},
      description = "New duration in minutes.")
  private Integer duration;

  @CommandLine.Option(
      names = {"--note", "-n"},
      description = "New note for the entry.")
  private String note;

  @CommandLine.Option(
      names = {"--type", "-t"},
      description = "New activity type for the entry.")
  private String activityType;

  @CommandLine.Option(
      names = {"--tags"},
      description = "New comma-separated list of tags (will replace existing tags).")
  private String tagsStr;

  @CommandLine.Option(
      names = {"--interactive"},
      description = "Enable interactive editing mode (prompts for all fields).")
  private boolean interactive;

  // Define common date/time formats for parsing flexibility
  private static final List<DateTimeFormatter> SUPPORTED_FORMATTERS =
      Arrays.asList(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME,
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

  @Override
  public void run() {
    Optional<TimeEntry> entryOpt = entryResolver.resolveEntry(id);
    if (entryOpt.isEmpty()) {
      System.err.println("❌ Entry with ID " + id + " not found.");
      return;
    }

    TimeEntry original = entryOpt.get();
    UUID entryId = UUID.fromString(id);
    if (interactive) {
      runInteractive(original, entryId);
    } else {
      runNonInteractive(original, entryId);
    }
  }

  /** Handles editing in non-interactive mode based on provided command-line options. */
  private void runNonInteractive(TimeEntry original, UUID entryId) {
    if (startTimeStr == null
        && duration == null
        && note == null
        && activityType == null
        && tagsStr == null) {
      System.out.println(
          "ℹ️ No update options provided (--duration, --note, --start-time, --type, --tags). Nothing to edit.");
      System.out.println(
          "Use --interactive for guided editing or provide specific options to change.");
      return;
    }

    LocalDateTime newStartTime = original.startTime();
    Integer newDuration = (duration != null) ? duration : original.durationMinutes();
    String newNote = (note != null) ? note : original.note();
    String newType = (activityType != null) ? activityType.toLowerCase() : original.activityType();
    Set<String> newTags = original.tags();

    // Parse start time if provided
    if (startTimeStr != null) {
      Optional<LocalDateTime> parsedTime = parseDateTime(startTimeStr);
      if (parsedTime.isPresent()) {
        newStartTime = parsedTime.get();
      } else {
        System.err.printf(
            "❌ Invalid format for --start: '%s'. Keeping original: %s%n",
            startTimeStr, original.startTime());
      }
    }

    if (tagsStr != null) {
      newTags = parseTags(tagsStr);
    }

    performUpdate(entryId, newStartTime, newDuration, newNote, newType, newTags, original);
  }

  /** Handles editing in interactive mode, prompting the user for each field. */
  private void runInteractive(TimeEntry original, UUID entryId) {
    try (Scanner scanner = new Scanner(System.in)) {
      System.out.println("\n--- Interactive Entry Editing ---\n");
      System.out.printf("Editing Entry ID: %s%n", original.id());

      LocalDateTime newStart = promptForDateTime(scanner, original.startTime());

      Integer newDuration = promptForInteger(scanner, original.durationMinutes());

      String newType =
          promptForString(scanner, "Activity type", original.activityType()).toLowerCase();

      Set<String> newTags = promptForTags(scanner, original.tags());

      String newNote = promptForString(scanner, "Note", original.note()); // Allow empty notes

      performUpdate(entryId, newStart, newDuration, newNote, newType, newTags, original);

    } catch (NoSuchElementException e) {
      System.err.println("\n❌ Input stream closed unexpectedly. Aborting edit.");
    } catch (Exception e) {
      System.err.println(
          "\n❌ An unexpected error occurred during interactive editing: " + e.getMessage());
    }
  }

  /** Executes the actual update in the EntryStore and commits via GitManager. */
  private void performUpdate(
      UUID entryId,
      LocalDateTime newStart,
      Integer newDuration,
      String newNote,
      String newType,
      Set<String> newTags,
      TimeEntry original) {
    if (Objects.equals(newStart, original.startTime())
        && Objects.equals(newDuration, original.durationMinutes())
        && Objects.equals(newNote, original.note())
        && Objects.equals(newType, original.activityType())
        && Objects.equals(newTags, original.tags())) {
      System.out.println("ℹ️ No changes detected. Entry remains unchanged.");
      return;
    }

    System.out.printf("Updating Entry %s...%n", entryId);
    boolean updated =
        entryStore.updateFullEntry(entryId, newStart, newDuration, newNote, newType, newTags);

    if (updated) {
      gitManager.commit("Edited entry " + entryId);
      System.out.println("✅ Entry updated successfully.");
    } else {
      System.err.println("❌ Failed to update entry in store. Check logs for details.");
    }
  }

  private Optional<LocalDateTime> parseDateTime(String dateTimeStr) {
    for (DateTimeFormatter formatter : SUPPORTED_FORMATTERS) {
      try {
        return Optional.of(LocalDateTime.parse(dateTimeStr, formatter));
      } catch (DateTimeParseException e) {
        // Try next format
      }
    }
    return Optional.empty();
  }

  private Set<String> parseTags(String tagsInput) {
    if (tagsInput == null || tagsInput.trim().isEmpty()) {
      return Collections.emptySet();
    }
    return Arrays.stream(tagsInput.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }

  private LocalDateTime promptForDateTime(Scanner scanner, LocalDateTime originalValue) {
    while (true) {
      System.out.printf(
          "%s [%s]: ", "Start time", originalValue.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        return originalValue;
      }
      Optional<LocalDateTime> parsed = parseDateTime(input);
      if (parsed.isPresent()) {
        return parsed.get();
      } else {
        System.out.println(
            "⚠️ Invalid date/time format. Please use formats like 'yyyy-MM-ddTHH:mm:ss' or 'yyyy-MM-dd HH:mm'. Try again.");
      }
    }
  }

  private Integer promptForInteger(Scanner scanner, Integer originalValue) {
    while (true) {
      System.out.printf("%s [%d]: ", "Duration in minutes", originalValue);
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        return originalValue;
      }
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException e) {
        System.out.println("⚠️ Invalid number format. Please enter an integer. Try again.");
      }
    }
  }

  private String promptForString(Scanner scanner, String fieldName, String originalValue) {
    String displayOriginal =
        (originalValue == null || originalValue.isEmpty()) ? "<empty>" : originalValue;
    System.out.printf("%s [%s]: ", fieldName, displayOriginal);
    String input = scanner.nextLine();
    return input.isEmpty() ? originalValue : input.trim();
  }

  private Set<String> promptForTags(Scanner scanner, Set<String> originalValue) {
    String originalTagsStr = String.join(",", originalValue);
    String displayOriginal = originalTagsStr.isEmpty() ? "<none>" : originalTagsStr;
    System.out.printf("%s [%s]: ", "Tags (comma-separated)", displayOriginal);
    String input = scanner.nextLine().trim();
    return input.isEmpty() ? originalValue : parseTags(input);
  }
}
