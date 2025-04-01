package io.ludovicianul.timi.command;

import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.git.GitManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "add",
    description =
        "Add a new time entry. Duration and type must be provided via options or using --interactive.",
    mixinStandardHelpOptions = true)
public class AddCommand implements Runnable {

  @Inject ConfigManager configManager;
  @Inject GitManager gitManager;
  @Inject EntryStore entryStore;

  @CommandLine.Option(
      names = {"--start", "-s"},
      description =
          "Start time (e.g., 'yyyy-MM-ddTHH:mm:ss' or 'yyyy-MM-dd HH:mm'). Defaults to now if omitted.")
  private String startStr;

  @CommandLine.Option(
      names = {"--duration", "-d"},
      description = "Duration in minutes.")
  private Integer duration;

  @CommandLine.Option(
      names = {"--type", "-t"},
      description = "Activity type (case-insensitive).")
  private String activityType;

  @CommandLine.Option(
      names = "--tags",
      description = "Comma-separated list of tags (case-insensitive).")
  private String tagsStr;

  @CommandLine.Option(
      names = {"--note", "-n"},
      description = "Note for the entry. Defaults to empty if omitted.")
  private String note;

  @CommandLine.Option(
      names = {"--interactive", "-i"},
      description = "Enable interactive mode to prompt for missing fields.")
  private boolean interactive;

  private static final List<DateTimeFormatter> SUPPORTED_FORMATTERS =
      Arrays.asList(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME,
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

  private static class InputData {
    LocalDateTime startTime;
    Integer durationMinutes;
    String type;
    Set<String> tags = Collections.emptySet();
    String note = "";
  }

  @Override
  public void run() {
    if (!interactive) {
      List<String> missingOptions = new ArrayList<>();
      if (this.duration == null) {
        missingOptions.add("--duration (-d)");
      }
      if (this.activityType == null || this.activityType.trim().isEmpty()) {
        missingOptions.add("--type (-t)");
      }

      if (!missingOptions.isEmpty()) {
        System.err.println(
            "❌ Missing required options in non-interactive mode: "
                + String.join(", ", missingOptions));
        System.err.println("   Provide these options or use the --interactive flag.");
        return;
      }
    }

    InputData inputs = gatherInputs();
    if (inputs == null) {
      return;
    }

    if (!validateInputs(inputs)) {
      return;
    }

    createAndSaveEntry(inputs);
  }

  /**
   * Gathers inputs from command-line args and potentially interactive prompts. Assumes required
   * fields for non-interactive mode have been validated *before* calling this. Returns InputData or
   * null if interactive mode fails or explicit args are invalid.
   */
  private InputData gatherInputs() {
    InputData data = new InputData();

    data.durationMinutes = this.duration;
    data.type = this.activityType;
    data.note = (this.note != null) ? this.note : "";

    if (this.startStr != null) {
      Optional<LocalDateTime> parsedTime = parseDateTime(this.startStr);
      if (parsedTime.isPresent()) {
        data.startTime = parsedTime.get();
      } else {
        System.err.printf(
            "❌ Invalid format provided for --start: '%s'. Aborting.%n", this.startStr);
        return null;
      }
    }

    if (this.tagsStr != null) {
      data.tags = parseTags(this.tagsStr);
    }

    if (interactive) {
      try (Scanner scanner = new Scanner(System.in)) {
        System.out.println("\n--- Interactive Entry Addition ---\n");

        if (data.startTime == null) {
          data.startTime = promptForDateTime(scanner);
        } else {
          System.out.printf(
              "Using provided start time: %s%n",
              data.startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        if (data.durationMinutes == null) {
          data.durationMinutes = promptForInteger(scanner);
        } else {
          System.out.printf("Using provided duration: %d minutes%n", data.durationMinutes);
        }

        if (data.type == null || data.type.trim().isEmpty()) {
          data.type =
              promptForString(
                  scanner,
                  String.format("Activity type (options: %s)", configManager.getActivityTypes()),
                  s -> configManager.isNotValidActivity(s));
        } else {
          System.out.printf("Using provided activity type: %s%n", data.type);
        }

        if (this.tagsStr == null) {
          data.tags =
              promptForTags(
                  scanner,
                  String.format("Tags (comma-separated, options: %s)", configManager.getTags()));
        } else {
          System.out.printf(
              "Using provided tags: %s%n",
              data.tags.isEmpty() ? "<none>" : String.join(",", data.tags));
        }

        if (this.note == null) {
          data.note = promptForString(scanner, "Note", s -> configManager.isNotValidTag(s));
        } else {
          System.out.printf(
              "Using provided note: %s%n", data.note.isEmpty() ? "<empty>" : data.note);
        }

      } catch (NoSuchElementException e) {
        System.err.println("\n❌ Input stream closed unexpectedly. Aborting add.");
        return null;
      } catch (Exception e) {
        System.err.println(
            "\n❌ An unexpected error occurred during interactive input: " + e.getMessage());
        return null;
      }
    } else {
      if (data.startTime == null) {
        data.startTime = LocalDateTime.now();
      }
    }

    if (data.type != null) {
      data.type = data.type.toLowerCase(Locale.ROOT);
    }

    return data;
  }

  /**
   * Validates the gathered inputs against business rules (ConfigManager, etc.). Returns true if
   * valid, false otherwise.
   */
  private boolean validateInputs(InputData data) {
    if (data.startTime == null) {
      return false;
    }
    if (data.durationMinutes == null) {
      return false;
    }
    if (data.type == null || data.type.trim().isEmpty()) {
      return false;
    }

    if (data.durationMinutes <= 0) {
      System.err.println("❌ Duration must be a positive number of minutes.");
      return false;
    }

    if (configManager.isNotValidActivity(data.type)) {
      System.err.printf(
          "❌ Invalid activity type: '%s'. Allowed: %s%n",
          data.type, configManager.getActivityTypes());
      return false;
    }

    for (String tag : data.tags) {
      if (configManager.isNotValidTag(tag)) {
        System.err.printf("❌ Invalid tag: '%s'. Allowed: %s%n", tag, configManager.getTags());
        return false;
      }
    }

    return true;
  }

  /** Creates the TimeEntry object, saves it, and commits. */
  private void createAndSaveEntry(InputData data) {
    TimeEntry entry =
        new TimeEntry(
            UUID.randomUUID(),
            data.startTime,
            data.durationMinutes,
            data.note,
            data.type,
            data.tags);

    System.out.printf(
        "Adding entry: Start=%s, Duration=%d, Type=%s, Tags=%s, Note='%s'%n",
        entry.startTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        entry.durationMinutes(),
        entry.activityType(),
        entry.tags().isEmpty() ? "<none>" : String.join(",", entry.tags()),
        entry.note());

    try {
      entryStore.saveEntry(entry);
      gitManager.commit(
          "Added: "
              + entry.startTime().format(DateTimeFormatter.ISO_DATE)
              + " ("
              + entry.activityType()
              + ")");
      System.out.println("✅ Entry added successfully.");
    } catch (Exception e) {
      System.err.println("❌ Failed to save entry or commit changes: " + e.getMessage());
    }
  }

  private Optional<LocalDateTime> parseDateTime(String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
      return Optional.of(LocalDateTime.now());
    }
    for (DateTimeFormatter formatter : SUPPORTED_FORMATTERS) {
      try {
        return Optional.of(LocalDateTime.parse(dateTimeStr.trim(), formatter));
      } catch (DateTimeParseException e) {
        /* Try next */
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

  private LocalDateTime promptForDateTime(Scanner scanner) {
    while (true) {
      String defaultStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      System.out.printf("%s [%s]: ", "Start time (yyyy-MM-ddTHH:mm, default now)", defaultStr);
      String input = scanner.nextLine().trim();
      Optional<LocalDateTime> parsed = parseDateTime(input);
      if (parsed.isPresent()) {
        return parsed.get();
      } else if (!input.isEmpty()) {
        System.out.println("⚠️ Invalid date/time format. Try again.");
      } else {
        System.out.println("⚠️ This field is required. Try again.");
      }
    }
  }

  private Integer promptForInteger(Scanner scanner) {
    while (true) {
      System.out.printf("%s [required]: ", "Duration in minutes");
      String input = scanner.nextLine().trim();
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException e) {
        System.out.println("⚠️ Invalid number format. Please enter an integer. Try again.");
      }
    }
  }

  private String promptForString(Scanner scanner, String prompt, Predicate<String> validator) {
    do {
      System.out.printf("%s [required]: ", prompt);
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        System.out.println("⚠️ This field is required. Try again.");
      } else if (validator.test(input)) {
        System.out.println("⚠️ Invalid input. Try again.");
      } else {
        return input;
      }
    } while (true);
  }

  private Set<String> promptForTags(Scanner scanner, String prompt) {
    System.out.printf("%s [%s]: ", prompt, "<empty>");
    String input = scanner.nextLine().trim();
    return input.isEmpty() ? Collections.emptySet() : parseTags(input);
  }
}
