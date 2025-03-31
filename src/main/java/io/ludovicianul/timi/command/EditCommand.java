package io.ludovicianul.timi.command;

import io.ludovicianul.timi.git.GitManager;
import io.ludovicianul.timi.persistence.EntryStore;
import jakarta.inject.Inject;
import java.util.UUID;
import picocli.CommandLine;

@CommandLine.Command(
    name = "edit",
    description = "Edit an existing entry by ID",
    mixinStandardHelpOptions = true)
public class EditCommand implements Runnable {
  @CommandLine.Option(
      names = {"--id", "-i"},
      required = true,
      description = "ID of the entry to edit")
  public String id;

  @CommandLine.Option(
      names = {"--duration", "-d"},
      description = "New duration in minutes")
  public Integer duration;

  @CommandLine.Option(
      names = {"--note", "-n"},
      description = "New note for the entry")
  public String note;

  @Inject GitManager gitManager;
  @Inject EntryStore entryStore;

  @Override
  public void run() {
    if (entryStore.updateById(UUID.fromString(id), duration, note)) {
      gitManager.commit("Edited entry " + id);
      System.out.println("Entry updated.");
    } else {
      System.out.println("Entry not found.");
    }
  }
}
