package io.ludovicianul.timi.git;

import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class GitManager {
  private final File repoDir = new File(System.getProperty("user.home"), ".timi");

  public GitManager() {
    try {
      if (!new File(repoDir, ".git").exists()) {
        executeGitCommand(List.of("init"));
      }
    } catch (Exception e) {
      throw new RuntimeException("Git init failed", e);
    }
  }

  public void commit(String message) {
    try {
      executeGitCommand(List.of("add", "."));
      executeGitCommand(List.of("commit", "-m", message));
    } catch (Exception e) {
      throw new RuntimeException("Git commit failed", e);
    }
  }

  private void executeGitCommand(List<String> commands) throws IOException, InterruptedException {
    List<String> baseCommand = new ArrayList<>(commands.size() + 3);
    baseCommand.add("git");
    baseCommand.add("-C");
    baseCommand.add(repoDir.getAbsolutePath());
    baseCommand.addAll(commands);

    ProcessBuilder processBuilder = new ProcessBuilder(baseCommand);
    processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

    Process process = processBuilder.start();
    boolean completed = process.waitFor(30, TimeUnit.SECONDS);

    if (!completed) {
      process.destroyForcibly();
      throw new RuntimeException("Git command timed out: " + baseCommand);
    }

    int exitCode = process.exitValue();
    if (exitCode != 0) {
      throw new RuntimeException(
          "Git command failed with exit code " + exitCode + ": " + baseCommand);
    }
  }
}
