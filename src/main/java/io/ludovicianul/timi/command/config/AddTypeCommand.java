package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import java.util.Locale;
import picocli.CommandLine;

@CommandLine.Command(
    name = "add-type",
    description = "Add a new activity type to config",
    mixinStandardHelpOptions = true)
public class AddTypeCommand implements Runnable {

  @CommandLine.Option(
      names = {"--name", "-n"},
      required = true,
      description = "Activity type to add")
  String name;

  @Inject ConfigManager configManager;

  @Override
  public void run() {
    String normalizedName = name.toLowerCase(Locale.ROOT);

    boolean added = configManager.addActivityType(normalizedName);
    if (added) {
      System.out.println("✅ Activity type added: " + normalizedName);
    } else {
      System.out.println("⚠️ Activity type already exists: " + normalizedName);
    }
  }
}
