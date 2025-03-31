package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(
    name = "show-tags",
    description = "Show all configured tags",
    mixinStandardHelpOptions = true)
public class ShowTagsCommand implements Runnable {
  @Inject ConfigManager configManager;

  @Override
  public void run() {
    System.out.println("Configured tags:");
    configManager.getTags().forEach(tag -> System.out.println("• " + tag));
  }
}
