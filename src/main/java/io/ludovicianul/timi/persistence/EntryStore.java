package io.ludovicianul.timi.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Singleton
public class EntryStore {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final Path baseDir = Path.of(System.getProperty("user.home"), ".timi", "entries");

  public void saveEntry(TimeEntry entry) {
    try {
      Files.createDirectories(baseDir);
      String fileName = entry.startTime().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".json";
      Path file = baseDir.resolve(fileName);

      List<TimeEntry> entries = new ArrayList<>();
      if (Files.exists(file)) {
        entries.addAll(Arrays.asList(mapper.readValue(file.toFile(), TimeEntry[].class)));
      }
      entries.add(entry);
      mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), entries);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save entry", e);
    }
  }

  public boolean updateById(UUID id, Integer newDuration, String newNote) {
    try (Stream<Path> files = Files.list(baseDir)) {
      for (Path file : files.toList()) {
        List<TimeEntry> entries = loadFromFile(file);
        boolean updated = false;

        List<TimeEntry> updatedEntries = new ArrayList<>();
        for (TimeEntry e : entries) {
          if (e.id().equals(id)) {
            TimeEntry updatedEntry =
                new TimeEntry(
                    e.id(),
                    e.startTime(),
                    newDuration != null ? newDuration : e.durationMinutes(),
                    newNote != null ? newNote : e.note(),
                    e.activityType(),
                    e.tags());
            updatedEntries.add(updatedEntry);
            updated = true;
          } else {
            updatedEntries.add(e);
          }
        }

        if (updated) {
          saveToFile(file, updatedEntries);
          return true;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to update entry by ID", e);
    }

    return false;
  }

  public boolean deleteById(UUID id) {
    try (Stream<Path> files = Files.list(baseDir)) {
      for (Path file : files.toList()) {
        List<TimeEntry> entries = loadFromFile(file);
        boolean removed = entries.removeIf(e -> e.id().equals(id));
        if (removed) {
          saveToFile(file, entries);
          return true;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete entry by ID", e);
    }

    return false;
  }

  public List<TimeEntry> loadFromFile(Path file) {
    try {
      if (!Files.exists(file)) return new ArrayList<>();
      return Arrays.asList(mapper.readValue(file.toFile(), TimeEntry[].class));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load from file: " + file, e);
    }
  }

  public void saveToFile(Path file, List<TimeEntry> entries) {
    try {
      mapper.writeValue(file.toFile(), entries);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save to file: " + file, e);
    }
  }

  public List<TimeEntry> loadAllEntries(String month) {
    List<TimeEntry> all = new ArrayList<>();
    if (!Files.exists(baseDir)) {
      return all;
    }

    try (Stream<Path> files = Files.list(baseDir)) {
      Stream<Path> filtered =
          (month != null) ? files.filter(f -> f.getFileName().toString().startsWith(month)) : files;

      for (Path path : filtered.toList()) {
        all.addAll(loadFromFile(path));
      }

      return all;
    } catch (IOException e) {
      throw new RuntimeException("Failed to load entries", e);
    }
  }
}
