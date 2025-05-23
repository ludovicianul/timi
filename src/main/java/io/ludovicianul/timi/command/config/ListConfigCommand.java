package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    description = "List configured activity types and tags and advanced configuration",
    mixinStandardHelpOptions = true)
public class ListConfigCommand implements Runnable {
  @Inject ConfigManager configManager;

  @Override
  public void run() {
    System.out.println("\n📂 Configured Activity Types:");
    configManager.getActivityTypes().forEach(type -> System.out.println("  • " + type));

    System.out.println("\n🏷  Configured Tags:");
    configManager.getTags().forEach(tag -> System.out.println("  • " + tag));

    System.out.println("\n\uD83D\uDD16  Configured Meta Tags:");
    configManager.getMetaTags().forEach(tag -> System.out.println("  • " + tag));

    System.out.println("\n⚙️  Advanced Settings:");
    System.out.println("  • gitEnabled: " + configManager.isGitEnabled());
    System.out.println("  • deepWorkValue: " + configManager.getDeepWorkValue());
    System.out.println("  • focusedWorkValue: " + configManager.getFocusedWorkValue());
    System.out.println("  • shortDurationThreshold: " + configManager.getShortDurationThreshold());
    System.out.println("  • shortDurationThreshold: " + configManager.getShortDurationThreshold());
    System.out.println("  • roundSessionMinutes: " + configManager.getRoundSessionMinutes());
  }
}
