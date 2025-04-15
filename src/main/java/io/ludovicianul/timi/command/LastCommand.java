package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import io.ludovicianul.timi.persistence.UndoAction;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import picocli.CommandLine;

@CommandLine.Command(
    name = "last",
    description = "Show the last action that can be undone",
    mixinStandardHelpOptions = true)
public class LastCommand implements Runnable {

  private final Path lastActionFile =
      Path.of(System.getProperty("user.home"), ".timi", "history", "last-action.json");

  @Inject EntryStore entryStore;

  @Override
  public void run() {
    if (!lastActionFile.toFile().exists()) {
      System.out.println("📭 No undoable action found.");
      return;
    }

    try {
      UndoAction action = entryStore.getLastAction();

      System.out.printf(
          "\n🕘 Last Action: '%s' at %s%n",
          action.action(),
          action.timestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

      if (action.entryAfter() != null) {
        printEntry("After", action.entryAfter());
      }
      if (action.entryBefore() != null) {
        printEntry("Before", action.entryBefore());
      }
    } catch (Exception e) {
      System.out.println("❌ Failed to load last action: " + e.getMessage());
    }
  }

  private void printEntry(String label, TimeEntry entry) {
    System.out.printf("\n%s:%n", label);
    System.out.printf("  ID: %s%n", entry.id());
    System.out.printf("  Time: %s%n", entry.startTime());
    System.out.printf("  Duration: %d min%n", entry.durationMinutes());
    System.out.printf("  Type: %s%n", entry.activityType());
    System.out.printf("  Tags: %s%n", String.join(", ", entry.tags()));
    System.out.printf("  Meta Tags: %s%n", String.join(", ", entry.metaTags()));
    System.out.printf("  Note: %s%n", entry.note());
  }
}
