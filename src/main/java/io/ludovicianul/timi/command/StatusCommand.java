package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.session.Session;
import io.ludovicianul.timi.persistence.session.SessionManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import picocli.CommandLine.*;

@Command(
    name = "status",
    description = "Check current session status",
    mixinStandardHelpOptions = true)
public class StatusCommand implements Runnable {

  private final SessionManager sessionManager = new SessionManager();

  @Override
  public void run() {
    Optional<Session> opt = sessionManager.load();
    if (opt.isEmpty()) {
      System.out.println("📭 No active session.");
      return;
    }

    Session s = opt.get();
    System.out.println("\n🟢 Active Session");
    System.out.println("----------------------");
    System.out.printf("Started: %s%n", s.start());
    System.out.printf("Paused: %s%n", s.paused() ? "Yes" : "No");

    long activeSeconds =
        Duration.between(s.start(), LocalDateTime.now()).getSeconds() - s.totalPausedSeconds();
    if (s.paused() && s.pausedAt() != null) {
      activeSeconds -= Duration.between(s.pausedAt(), LocalDateTime.now()).getSeconds();
    }

    long minutes = activeSeconds / 60;
    System.out.printf("Active Time: %d min%n", minutes);

    if (s.type() != null) System.out.printf("Type: %s%n", s.type());
    if (s.tags() != null && !s.tags().isEmpty())
      System.out.printf("Tags: %s%n", String.join(", ", s.tags()));
    if (s.note() != null && !s.note().isBlank()) System.out.printf("Note: %s%n", s.note());
    System.out.println("----------------------");
  }
}
