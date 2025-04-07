package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import java.util.Locale;
import picocli.CommandLine;

@CommandLine.Command(
    name = "add-meta-tag",
    description = "Add a new meta tag to config",
    mixinStandardHelpOptions = true)
public class AddMetaTagCommand implements Runnable {

  @CommandLine.Option(
      names = {"--name", "-n"},
      required = true,
      description = "Meta tag name to add")
  String name;

  @Inject ConfigManager configManager;

  @Override
  public void run() {
    String normalizedName = name.toLowerCase(Locale.ROOT);
    boolean added = configManager.addMetaTag(normalizedName);
    if (added) {
      System.out.println("\n✅ Meta tag added: " + normalizedName);
    } else {
      System.out.println("\n⚠️ Meta tag already exists: " + normalizedName);
    }
  }
}
