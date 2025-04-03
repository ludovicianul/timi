package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(
    name = "show-types",
    description = "Show all configured activity types",
    mixinStandardHelpOptions = true)
public class ShowTypesCommand implements Runnable {
  @Inject ConfigManager configManager;

  @Override
  public void run() {
    System.out.println("📂 Configured activity types:");
    configManager.getActivityTypes().forEach(type -> System.out.println("• " + type));
  }
}
