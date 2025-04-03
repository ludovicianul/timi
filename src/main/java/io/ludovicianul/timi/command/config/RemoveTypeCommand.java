package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import java.util.Locale;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remove-type",
    description = "Remove an activity type from the config",
    mixinStandardHelpOptions = true)
public class RemoveTypeCommand implements Runnable {

  @CommandLine.Option(
      names = {"--name", "-n"},
      required = true,
      description = "Activity type to remove")
  String name;

  @Inject ConfigManager configManager;

  @Override
  public void run() {
    String normalizedName = name.toLowerCase(Locale.ROOT);

    boolean removed = configManager.removeActivityType(normalizedName);
    if (removed) {
      System.out.println("✅ Activity type removed: " + normalizedName);
    } else {
      System.out.println("⚠️ Activity type not found: " + normalizedName);
    }
  }
}
