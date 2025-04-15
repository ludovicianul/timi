package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.UndoAction;
import jakarta.inject.Inject;
import java.nio.file.Path;
import picocli.CommandLine;

@CommandLine.Command(
    name = "undo",
    description = "Undo the last action (add/edit/delete)",
    mixinStandardHelpOptions = true)
public class UndoCommand implements Runnable {

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

      switch (action.action()) {
        case "add" -> {
          entryStore.deleteById(action.entryAfter().id());
          System.out.printf(
              "✅ Undone: Added entry %s has been removed.%n", action.entryAfter().id());
        }
        case "delete" -> {
          entryStore.saveEntry(action.entryBefore());
          System.out.printf(
              "✅ Undone: Deleted entry %s has been restored.%n", action.entryBefore().id());
        }
        case "edit" -> {
          entryStore.updateFullEntry(
              action.entryBefore().id(),
              action.entryBefore().startTime(),
              action.entryBefore().durationMinutes(),
              action.entryBefore().note(),
              action.entryBefore().activityType(),
              action.entryBefore().tags(),
              action.entryBefore().metaTags());
          System.out.printf(
              "✅ Undone: Entry %s reverted to previous state.%n", action.entryBefore().id());
        }
        default -> System.out.printf("❌ Unknown action type: %s%n", action.action());
      }

      lastActionFile.toFile().delete();

    } catch (Exception e) {
      System.out.println("❌ Failed to undo last action: " + e.getMessage());
    }
  }
}
