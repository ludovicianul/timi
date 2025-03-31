package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import picocli.CommandLine;

@CommandLine.Command(
    name = "prune",
    description = "Remove unused tags and types from config",
    mixinStandardHelpOptions = true)
public class PruneConfigCommand implements Runnable {
  @Inject ConfigManager configManager;
  @Inject EntryStore entryStore;

  @Override
  public void run() {
    Set<String> usedTags =
        entryStore.loadAllEntries(null).stream()
            .flatMap(e -> e.tags().stream())
            .collect(Collectors.toSet());
    Set<String> usedTypes =
        entryStore.loadAllEntries(null).stream()
            .map(TimeEntry::activityType)
            .collect(Collectors.toSet());

    List<String> unusedTags =
        configManager.getTags().stream().filter(t -> !usedTags.contains(t)).toList();
    List<String> unusedTypes =
        configManager.getActivityTypes().stream().filter(t -> !usedTypes.contains(t)).toList();

    if (unusedTags.isEmpty() && unusedTypes.isEmpty()) {
      System.out.println("✅ No unused tags or activity types found.");
      return;
    }

    unusedTags.forEach(configManager::removeTag);
    unusedTypes.forEach(configManager::removeActivityType);

    System.out.println("🧹 Pruned unused tags and activity types:");
    unusedTags.forEach(tag -> System.out.println("  - Removed tag: " + tag));
    unusedTypes.forEach(type -> System.out.println("  - Removed type: " + type));
  }
}
