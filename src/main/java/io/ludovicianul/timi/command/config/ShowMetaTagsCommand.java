package io.ludovicianul.timi.command.config;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(
    name = "show-meta-tags",
    description = "Show all configured tags",
    mixinStandardHelpOptions = true)
public class ShowMetaTagsCommand implements Runnable {
  @Inject ConfigManager configManager;

  @Override
  public void run() {
    System.out.println("\n🏷  Configured meta tags:");
    configManager.getMetaTags().forEach(tag -> System.out.println("• " + tag));
  }
}
