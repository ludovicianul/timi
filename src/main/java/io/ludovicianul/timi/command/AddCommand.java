package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.*;

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
      names = {"--tags", "--tag"},
      description = "Comma-separated list of tags (case-insensitive).",
      split = ",")
  private List<String> tags;

  @CommandLine.Option(
      names = {"--note", "-n"},
      description = "Note for the entry. Defaults to empty if omitted.")
  private String note;

  @CommandLine.Option(
      names = {"--interactive", "-i"},
      description = "Enable interactive mode to prompt for missing fields.")
  private boolean interactive;

  private static class InputData {
    LocalDateTime startTime;
    Integer durationMinutes;
    String type;
    Set<String> tags = Collections.emptySet();
    String note = "";
  }

  @Override
  public void run() {
    try (Scanner scanner = new Scanner(System.in)) {
      if (!interactive) {
        List<String> missingOptions = new ArrayList<>();
        if (this.duration == null) missingOptions.add("--duration (-d)");
        if (this.activityType == null || this.activityType.trim().isEmpty())
          missingOptions.add("--type (-t)");

        if (!missingOptions.isEmpty()) {
          System.err.println("❌ Missing options: " + String.join(", ", missingOptions));
          System.err.println("Use --interactive or provide these options explicitly.");
          return;
        }
      }

      InputData inputs = gatherInputs(scanner);
      if (inputs == null || !validateInputs(inputs)) {
        return;
      }

      if (interactive && !confirmInputs(scanner, inputs)) {
        System.out.println("❌ Entry addition cancelled.");
        return;
      }

      createAndSaveEntry(inputs);
    } catch (Exception e) {
      System.err.println("❌ Unexpected error: " + e.getMessage());
    }
  }

  private boolean confirmInputs(Scanner scanner, InputData inputs) {
    System.out.println("\nPlease confirm entry details:");
    System.out.printf(
        "• Start Time: %s%n", inputs.startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    System.out.printf("• Duration: %d minutes%n", inputs.durationMinutes);
    System.out.printf("• Activity Type: %s%n", inputs.type);
    System.out.printf(
        "• Tags: %s%n", inputs.tags.isEmpty() ? "<none>" : String.join(", ", inputs.tags));
    System.out.printf("• Note: %s%n", inputs.note.isEmpty() ? "<empty>" : inputs.note);

    System.out.print("\nConfirm addition? (y/N): ");
    String confirm = scanner.nextLine().trim().toLowerCase();
    return confirm.equals("y");
  }

  private InputData gatherInputs(Scanner scanner) {
    InputData data = new InputData();
    data.durationMinutes = this.duration;
    data.type = this.activityType;
    data.note = (this.note != null) ? this.note : "";

    if (this.startStr != null) {
      try {
        data.startTime = parseDateTime(this.startStr);
      } catch (DateTimeParseException e) {
        System.err.printf("❌ Invalid format for --start: '%s'.%n", this.startStr);
        return null;
      }
    }

    if (this.tags != null) {
      data.tags = tags.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    if (interactive) {
      System.out.println("\n--- Interactive Entry Addition ---\n");

      if (data.startTime == null) {
        data.startTime = promptForDateTime(scanner);
      } else {
        System.out.printf("Using provided start time: %s%n", data.startTime);
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
                configManager::isNotValidActivity);
      } else {
        System.out.printf("Using provided activity type: %s%n", data.type);
      }

      if (this.tags == null) {
        data.tags =
            promptForTags(scanner, String.format("Tags (options: %s)", configManager.getTags()));
      } else {
        System.out.printf(
            "Using provided tags: %s%n",
            data.tags.isEmpty() ? "<none>" : String.join(",", data.tags));
      }
      if (this.note == null) {
        data.note = promptForString(scanner, "Note", s -> false);
      } else {
        System.out.printf("Using provided note: %s%n", data.note.isEmpty() ? "<empty>" : data.note);
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
        "\nAdding entry: Start=%s, Duration=%d, Type=%s, Tags=%s, Note='%s'%n",
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
      if (input.isEmpty()) {
        return LocalDateTime.now();
      }
      try {
        return parseDateTime(input);
      } catch (DateTimeParseException e) {
        System.out.println("⚠️ Invalid date/time format. Try again.");
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
    while (true) {
      System.out.printf("%s [required]: ", prompt);
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        System.out.println("⚠️ This field is required. Try again.");
      } else if (validator.test(input)) {
        System.out.println("⚠️ Invalid input. Try again.");
      } else {
        return input;
      }
    }
  }

  private Set<String> promptForTags(Scanner scanner, String prompt) {
    while (true) {
      System.out.printf("%s [%s]: ", prompt, "<empty>");
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        return Collections.emptySet();
      }
      Set<String> inputTags = parseTags(input);
      Set<String> difference = new HashSet<>(inputTags);
      difference.removeAll(configManager.getTags());
      if (difference.isEmpty()) {
        return inputTags;
      } else {
        System.err.printf(
            "❌ Invalid tag(s): '%s'. Allowed: %s%n", difference, configManager.getTags());
      }
    }
  }
}
