package io.ludovicianul.timi.command;

import io.ludovicianul.timi.git.GitManager;
import io.ludovicianul.timi.persistence.EntryStore;
import jakarta.inject.Inject;
import java.util.UUID;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    description = "Delete an entry by ID",
    mixinStandardHelpOptions = true)
public class DeleteCommand implements Runnable {
  @CommandLine.Option(names = "--id", required = true, description = "ID of the entry to delete")
  public String id;

  @Inject GitManager gitManager;
  @Inject EntryStore entryStore;

  @Override
  public void run() {
    if (entryStore.deleteById(UUID.fromString(id))) {
      gitManager.commit("Deleted entry " + id);
      System.out.println("Entry deleted.");
    } else {
      System.out.println("Entry not found.");
    }
  }
}
