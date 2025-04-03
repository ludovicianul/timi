package io.ludovicianul.timi.command;

import io.ludovicianul.timi.git.GitManager;
import io.ludovicianul.timi.persistence.EntryResolver;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    description = "Delete an entry by ID",
    mixinStandardHelpOptions = true)
public class DeleteCommand implements Runnable {

  @CommandLine.Option(
      names = {"--id", "-i"},
      required = true,
      description = "ID of the entry to delete")
  public String id;

  @CommandLine.Option(
      names = {"--force", "-f"},
      description = "Force deletion without confirmation")
  boolean force;

  @Inject GitManager gitManager;
  @Inject EntryStore entryStore;
  @Inject EntryResolver entryResolver;

  @Override
  public void run() {
    Optional<TimeEntry> entryOpt = entryResolver.resolveEntry(id);
    if (entryOpt.isEmpty()) return;

    TimeEntry entry = entryOpt.get();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    System.out.println("\n🗑 You are about to delete the following entry:");
    System.out.printf("• Date: %s%n", entry.startTime().format(formatter));
    System.out.printf(
        "• Duration: %dh %02dm%n", entry.durationMinutes() / 60, entry.durationMinutes() % 60);
    System.out.printf("• Type: %s%n", entry.activityType());
    System.out.printf("• Tags: %s%n", String.join(", ", entry.tags()));
    System.out.printf("• Note: %s%n", entry.note());

    if (!force) {
      System.out.print("\nAre you sure you want to delete this entry? (y/N): ");
      Scanner scanner = new Scanner(System.in);
      String confirm = scanner.nextLine().trim().toLowerCase();
      if (!confirm.equals("y")) {
        System.out.println("❌ Deletion cancelled.");
        return;
      }
    }

    if (entryStore.deleteById(UUID.fromString(id))) {
      gitManager.commit("Deleted entry " + id);
      System.out.println("✅ Entry deleted.");
    } else {
      System.out.println("❌ Failed to delete entry.");
    }
  }
}
