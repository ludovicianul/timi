package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import java.util.Locale;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remove-tag",
    description = "Remove a tag from the config",
    mixinStandardHelpOptions = true)
public class RemoveTagCommand implements Runnable {

  @CommandLine.Option(
      names = {"--name", "-n"},
      required = true,
      description = "Tag to remove")
  String name;

  @Inject ConfigManager configManager;

  @Override
  public void run() {
    String normalizedName = name.toLowerCase(Locale.ROOT);
    boolean removed = configManager.removeTag(normalizedName);
    if (removed) {
      System.out.println("\n✅ Tag removed: " + normalizedName);
    } else {
      System.out.println("\n⚠️ Tag not found: " + normalizedName);
    }
  }
}
