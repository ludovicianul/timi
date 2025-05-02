package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.session.SessionManager;
import picocli.CommandLine.*;

@Command(
    name = "abort",
    description = "Abort the current session without saving",
    mixinStandardHelpOptions = true)
public class AbortCommand implements Runnable {

  private final SessionManager sessionManager = new SessionManager();

  @Override
  public void run() {
    if (!sessionManager.exists()) {
      System.out.println("⚠️ No active session to abort.");
      return;
    }

    sessionManager.clear();
    System.out.println("🗑️ Session aborted and discarded.");
  }
}
