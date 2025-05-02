package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.session.Session;
import io.ludovicianul.timi.persistence.session.SessionManager;
import java.time.LocalDateTime;
import picocli.CommandLine.*;

@Command(name = "pause", description = "Pause the current session", mixinStandardHelpOptions = true)
public class PauseCommand implements Runnable {

  private final SessionManager sessionManager = new SessionManager();

  @Override
  public void run() {
    var opt = sessionManager.load();
    if (opt.isEmpty()) {
      System.out.println("⚠️ No active session to pause.");
      return;
    }

    Session s = opt.get();
    if (s.paused()) {
      System.out.println("⏸️ Session is already paused.");
      return;
    }

    sessionManager.save(
        new Session(
            s.id(),
            s.start(),
            true,
            LocalDateTime.now(),
            s.totalPausedSeconds(),
            s.type(),
            s.tags(),
            s.note()));

    System.out.println("⏸️ Session paused.");
  }
}
