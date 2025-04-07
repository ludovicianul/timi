package io.ludovicianul.timi.command;

import io.ludovicianul.timi.version.VersionProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import picocli.CommandLine.Command;

@Command(
    name = "info",
    description = "Display system and application metadata",
    mixinStandardHelpOptions = true)
public class InfoCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("\n📦 Timi CLI - Environment Info\n");

    System.out.println("🧩 Version:        " + VersionProvider.VERSION);
    System.out.println("🕒 Built at:       " + VersionProvider.DATE);
    System.out.println("📁 Config path:    ~/.timi/config.json");
    System.out.println("📁 Data path:      ~/.timi/entries/");

    Properties props = System.getProperties();
    System.out.println(
        "\n🖥️ Java Runtime:   "
            + props.getProperty("java.runtime.name")
            + " "
            + props.getProperty("java.runtime.version"));
    System.out.println("🖥️ JVM Vendor:     " + props.getProperty("java.vm.vendor"));
    System.out.println(
        "🖥️ OS:             "
            + props.getProperty("os.name")
            + " ("
            + props.getProperty("os.arch")
            + ")");
    System.out.println("🖥️ User:           " + props.getProperty("user.name"));

    try (var paths = Files.list(Path.of(System.getProperty("user.home"), ".timi", "entries"))) {
      long entryCount =
          paths
              .filter(p -> p.toString().endsWith(".json") && !p.toString().endsWith("index.json"))
              .count();
      System.out.println("\n📊 Stored monthly entry files: " + entryCount);
    } catch (IOException e) {
      System.out.println("⚠️ Could not read entry directory.");
    }

    System.out.println("\n✅ Ready to track. \n");
  }
}
