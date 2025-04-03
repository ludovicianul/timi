package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "validate",
    description = "Validate entries against config values",
    mixinStandardHelpOptions = true)
public class ValidateConfigCommand implements Runnable {
  @Inject ConfigManager configManager;
  @Inject EntryStore entryStore;

  @Override
  public void run() {
    List<String> errors = new ArrayList<>();

    for (TimeEntry entry : entryStore.loadAllEntries(null)) {
      if (configManager.isNotValidActivity(entry.activityType())) {
        errors.add("⚠️ Invalid activity type in entry " + entry.id() + ": " + entry.activityType());
      }
      for (String tag : entry.tags()) {
        if (configManager.isNotValidTag(tag)) {
          errors.add("⚠️ Invalid tag in entry " + entry.id() + ": " + tag);
        }
      }
    }

    if (errors.isEmpty()) {
      System.out.println("\n✅ All entries are valid.");
    } else {
      System.out.println("\n❌ Found " + errors.size() + " invalid entries:");
      errors.forEach(System.out::println);
    }
  }
}
