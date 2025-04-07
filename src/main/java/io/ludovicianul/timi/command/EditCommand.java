package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.parseDateTime;

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
      names = {"--id"},
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
      names = {"--tags", "-tag"},
      description = "New comma-separated list of tags (will replace existing tags).",
      split = ",")
  private List<String> tags;

  @CommandLine.Option(
      names = {"--interactive", "-i"},
      description = "Enable interactive editing mode (prompts for all fields).")
  private boolean interactive;

  @Override
  public void run() {
    Optional<TimeEntry> entryOpt = entryResolver.resolveEntry(id);
    if (entryOpt.isEmpty()) {
      System.err.println("❌ Entry with ID " + id + " not found.");
      return;
    }

    TimeEntry original = entryOpt.get();
    UUID entryId = UUID.fromString(id);
    EntryFields originalFields = EntryFields.from(original);

    EntryFields updatedFields =
        interactive ? runInteractive(originalFields) : runNonInteractive(originalFields);

    if (updatedFields == null) {
      return;
    }

    processUpdate(entryId, originalFields, updatedFields);
  }

  private EntryFields runNonInteractive(EntryFields original) {
    if (startTimeStr == null
        && duration == null
        && note == null
        && activityType == null
        && tags == null) {
      System.out.println(
          "ℹ️ No update options provided (--duration, --note, --start, --type, --tags). Nothing to edit.");
      System.out.println(
          "Use --interactive for guided editing or provide specific options to change.");
      return null;
    }

    LocalDateTime newStartTime = original.startTime();
    Integer newDuration = (duration != null) ? duration : original.durationMinutes();
    String newNote = (note != null) ? note : original.note();
    String newType = (activityType != null) ? activityType.toLowerCase() : original.activityType();
    Set<String> newTags = original.tags();

    if (startTimeStr != null) {
      try {
        newStartTime = parseDateTime(startTimeStr);
      } catch (DateTimeParseException e) {
        System.err.printf(
            "❌ Invalid format for --start: '%s'. Keeping original: %s%n",
            startTimeStr, original.startTime());
      }
    }

    if (tags != null) {
      newTags = tags.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    EntryFields updatedFields =
        new EntryFields(newStartTime, newDuration, newNote, newType, newTags);

    if (!showChangesAndConfirm(original, updatedFields)) {
      return null;
    }

    return updatedFields;
  }

  private EntryFields runInteractive(EntryFields original) {
    try (Scanner scanner = new Scanner(System.in)) {
      System.out.println("\n--- Interactive Entry Editing ---\n");
      System.out.printf("Editing Entry ID: %s%n", id);

      LocalDateTime newStart = promptForDateTime(scanner, original.startTime());
      Integer newDuration = promptForInteger(scanner, original.durationMinutes());
      String newType =
          promptForString(scanner, "Activity type", original.activityType()).toLowerCase();
      Set<String> newTags = promptForTags(scanner, original.tags());
      String newNote = promptForString(scanner, "Note", original.note());

      EntryFields updatedFields = new EntryFields(newStart, newDuration, newNote, newType, newTags);

      if (!showChangesAndConfirm(original, updatedFields)) {
        return null;
      }

      return updatedFields;
    } catch (NoSuchElementException e) {
      System.err.println("\n❌ Input stream closed unexpectedly. Aborting edit.");
      return null;
    } catch (Exception e) {
      System.err.println(
          "\n❌ An unexpected error occurred during interactive editing: " + e.getMessage());
      return null;
    }
  }

  private boolean showChangesAndConfirm(EntryFields original, EntryFields updated) {
    System.out.println("\nPlease review the changes:");
    System.out.printf(
        "• Original Start Time: %s -> New Start Time: %s%n",
        original.formatStartTime(), updated.formatStartTime());
    System.out.printf(
        "• Original Duration: %d -> New Duration: %d%n",
        original.durationMinutes(), updated.durationMinutes());
    System.out.printf(
        "• Original Activity Type: %s -> New Activity Type: %s%n",
        original.activityType(), updated.activityType());
    System.out.printf("• Original Tags: %s -> New Tags: %s%n", original.tags(), updated.tags());
    System.out.printf("• Original Note: %s -> New Note: %s%n", original.note(), updated.note());

    System.out.print("\nConfirm update? (y/N): ");
    Scanner scanner = new Scanner(System.in);
    String confirm = scanner.nextLine().trim().toLowerCase();

    if (!confirm.equals("y")) {
      System.out.println("ℹ️ No changes made. Update cancelled.");
      return false;
    }

    return true;
  }

  private void processUpdate(UUID entryId, EntryFields original, EntryFields updated) {
    if (updated.isEqualTo(original)) {
      System.out.println("ℹ️ No changes detected. Entry remains unchanged.");
      return;
    }

    System.out.printf("Updating Entry %s...%n", entryId);
    boolean success =
        entryStore.updateFullEntry(
            entryId,
            updated.startTime(),
            updated.durationMinutes(),
            updated.note(),
            updated.activityType(),
            updated.tags());

    if (success) {
      gitManager.commit("Edited entry " + entryId);
      System.out.println("✅ Entry updated successfully.");
    } else {
      System.err.println("❌ Failed to update entry in store. Check logs for details.");
    }
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
      try {
        return parseDateTime(input);
      } catch (DateTimeParseException e) {
        System.out.println("⚠️ Invalid date/time format. Please try again.");
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

  private record EntryFields(
      LocalDateTime startTime,
      Integer durationMinutes,
      String note,
      String activityType,
      Set<String> tags) {

    static EntryFields from(TimeEntry entry) {
      return new EntryFields(
          entry.startTime(),
          entry.durationMinutes(),
          entry.note(),
          entry.activityType(),
          entry.tags());
    }

    String formatStartTime() {
      return startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    boolean isEqualTo(EntryFields other) {
      return Objects.equals(startTime, other.startTime)
          && Objects.equals(durationMinutes, other.durationMinutes)
          && Objects.equals(note, other.note)
          && Objects.equals(activityType, other.activityType)
          && Objects.equals(tags, other.tags);
    }
  }
}
