package io.ludovicianul.timi.persistence.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Singleton
public class SessionManager {
  private final Path SESSION_FILE =
      Path.of(System.getProperty("user.home"), ".timi", "session.json");
  private final ObjectMapper mapper;

  public SessionManager() {
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
  }

  public Optional<Session> load() {
    if (!Files.exists(SESSION_FILE)) return Optional.empty();
    try {
      return Optional.of(mapper.readValue(SESSION_FILE.toFile(), Session.class));
    } catch (IOException e) {
      System.err.println("❌ Failed to load session: " + e.getMessage());
      return Optional.empty();
    }
  }

  public void save(Session session) {
    try {
      Files.createDirectories(SESSION_FILE.getParent());
      mapper.writerWithDefaultPrettyPrinter().writeValue(SESSION_FILE.toFile(), session);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save session", e);
    }
  }

  public void clear() {
    try {
      Files.deleteIfExists(SESSION_FILE);
    } catch (IOException e) {
      System.err.println("❌ Failed to clear session: " + e.getMessage());
    }
  }

  public boolean exists() {
    return Files.exists(SESSION_FILE);
  }
}
