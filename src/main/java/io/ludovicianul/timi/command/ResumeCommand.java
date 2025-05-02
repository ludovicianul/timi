package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.session.Session;
import io.ludovicianul.timi.persistence.session.SessionManager;
import java.time.Duration;
import java.time.LocalDateTime;
import picocli.CommandLine.*;

@Command(name = "resume", description = "Resume a paused session", mixinStandardHelpOptions = true)
public class ResumeCommand implements Runnable {

  private final SessionManager sessionManager = new SessionManager();

  @Override
  public void run() {
    var opt = sessionManager.load();
    if (opt.isEmpty()) {
      System.out.println("⚠️ No session to resume.");
      return;
    }

    Session s = opt.get();
    if (!s.paused()) {
      System.out.println("▶️ Session is not paused.");
      return;
    }

    long pausedSeconds = Duration.between(s.pausedAt(), LocalDateTime.now()).getSeconds();
    long newTotal = s.totalPausedSeconds() + pausedSeconds;

    sessionManager.save(
        new Session(s.id(), s.start(), false, null, newTotal, s.type(), s.tags(), s.note()));

    System.out.printf("▶️ Resumed session. Total paused time: %d minutes%n", newTotal / 60);
  }
}
