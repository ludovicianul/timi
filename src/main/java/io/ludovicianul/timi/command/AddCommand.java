package io.ludovicianul.timi.command;

import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.git.GitManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "add",
    description = "Add a new time entry",
    mixinStandardHelpOptions = true)
public class AddCommand implements Runnable {
  @CommandLine.Option(
      names = {"--start", "-s"},
      description = "Start time in ISO format")
  public String start = LocalDateTime.now().toString();

  @CommandLine.Option(
      names = {"--duration", "-d"},
      required = true,
      description = "Duration in minutes")
  public int duration;

  @CommandLine.Option(
      names = {"--type", "-t"},
      required = true,
      description = "Activity type (case-insensitive)")
  public String activityType;

  @CommandLine.Option(
      names = "--tags",
      split = ",",
      description = "Comma-separated list of tags",
      required = true)
  public Set<String> tags;

  @CommandLine.Option(
      names = {"--note", "-n"},
      description = "Note for the entry")
  public String note;

  @Inject ConfigManager configManager;
  @Inject GitManager gitManager;
  @Inject EntryStore entryStore;

  @Override
  public void run() {
    String normalizedType = activityType.toLowerCase(Locale.ROOT);
    Set<String> normalizedTags =
        tags != null
            ? tags.stream().map(String::toLowerCase).collect(Collectors.toSet())
            : Set.of();

    if (configManager.isNotValidActivity(normalizedType)) {
      System.out.printf(
          "⚠️ Invalid activity type: '%s'. Allowed: %s%n",
          normalizedType, configManager.getActivityTypes());
      return;
    }

    for (String tag : normalizedTags) {
      if (configManager.isNotValidTag(tag)) {
        System.out.printf("⚠️ Invalid tag: '%s'. Allowed: %s%n", tag, configManager.getTags());
        return;
      }
    }

    TimeEntry entry =
        new TimeEntry(
            UUID.randomUUID(),
            LocalDateTime.parse(start),
            duration,
            note,
            normalizedType,
            normalizedTags);

    entryStore.saveEntry(entry);
    gitManager.commit("Added: " + entry.startTime() + " (" + normalizedType + ")");
    System.out.println("✅ Entry added.");
  }
}
