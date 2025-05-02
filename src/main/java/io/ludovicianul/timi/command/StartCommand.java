package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.session.Session;
import io.ludovicianul.timi.persistence.session.SessionManager;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import picocli.CommandLine.*;

@Command(name = "start", description = "Start a new work session", mixinStandardHelpOptions = true)
public class StartCommand implements Runnable {

  @Option(names = "--type", description = "Optional activity type")
  String type;

  @Option(names = "--tags", split = ",", description = "Optional comma-separated tags")
  Set<String> tags;

  @Option(names = "--note", description = "Optional session note")
  String note;

  private final SessionManager sessionManager = new SessionManager();

  @Override
  public void run() {
    if (sessionManager.exists()) {
      System.out.println(
          "⚠️ A session is already in progress. Use 'timi status' to check or 'timi stop' to end it.");
      return;
    }

    Session session =
        new Session(
            UUID.randomUUID(),
            LocalDateTime.now(),
            false,
            null,
            0,
            type,
            tags != null ? tags : new HashSet<>(),
            note != null ? note : "");

    sessionManager.save(session);
    System.out.printf("✅ Session started at %s%n", session.start());
    if (type != null) System.out.printf("  Type: %s%n", type);
    if (tags != null && !tags.isEmpty()) System.out.printf("  Tags: %s%n", String.join(", ", tags));
    if (note != null && !note.isBlank()) System.out.printf("  Note: %s%n", note);
  }
}
