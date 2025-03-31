package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import java.util.Locale;
import picocli.CommandLine;

@CommandLine.Command(
    name = "add-tag",
    description = "Add a new tag to config",
    mixinStandardHelpOptions = true)
public class AddTagCommand implements Runnable {

  @CommandLine.Option(
      names = {"--name", "-n"},
      required = true,
      description = "Tag name to add")
  String name;

  @Inject ConfigManager configManager;

  @Override
  public void run() {
    String normalizedName = name.toLowerCase(Locale.ROOT);
    boolean added = configManager.addTag(normalizedName);
    if (added) {
      System.out.println("✅ Tag added: " + normalizedName);
    } else {
      System.out.println("⚠️ Tag already exists: " + normalizedName);
    }
  }
}
