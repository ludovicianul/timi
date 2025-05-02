package io.ludovicianul.timi.command;

import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import io.ludovicianul.timi.persistence.session.Session;
import io.ludovicianul.timi.persistence.session.SessionManager;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import picocli.CommandLine.*;

@Command(
    name = "stop",
    description = "Stop and save the current session",
    mixinStandardHelpOptions = true)
public class StopCommand implements Runnable {

  @Inject EntryStore entryStore;
  @Inject ConfigManager configManager;
  private final SessionManager sessionManager = new SessionManager();

  @Override
  public void run() {
    var opt = sessionManager.load();
    if (opt.isEmpty()) {
      System.out.println("⚠️ No active session to stop.");
      return;
    }

    Session s = opt.get();
    if (s.paused()) {
      System.out.println("⚠️ Session is paused. Resume first.");
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    long elapsedSeconds = Duration.between(s.start(), now).getSeconds() - s.totalPausedSeconds();
    int minutes = (int) (elapsedSeconds / 60);

    int roundTo = configManager.getRoundSessionMinutes(); // 0, 5, 10
    if (roundTo > 0) {
      minutes = ((minutes + roundTo / 2) / roundTo) * roundTo;
    }

    Scanner scanner = new Scanner(System.in);
    String type = s.type();
    while (type == null || type.isBlank() || configManager.isNotValidActivity(type)) {
      System.out.printf(
          "Type (options: %s): ", String.join(", ", configManager.getActivityTypes()));
      type = scanner.nextLine().trim();
    }

    Set<String> tags = s.tags();
    while (tags == null
        || tags.isEmpty()
        || tags.stream().anyMatch(t -> configManager.isNotValidTag(t))) {
      System.out.printf(
          "Tags (comma-separated, options: %s): ", String.join(", ", configManager.getTags()));
      String input = scanner.nextLine();
      tags =
          Set.of(input.split(",")).stream()
              .map(String::trim)
              .filter(s2 -> !s2.isEmpty())
              .collect(Collectors.toSet());
    }

    String note = s.note();
    if (note == null || note.isBlank()) {
      System.out.print("Note: ");
      note = scanner.nextLine().trim();
    }

    UUID id = UUID.randomUUID();
    TimeEntry entry = new TimeEntry(id, s.start(), minutes, note, type, tags, Set.of());

    entryStore.saveEntry(entry);
    sessionManager.clear();

    System.out.printf("✅ Session saved: %s (%d min) [%s]%n", type, minutes, id);
  }
}
