package io.ludovicianul.timi.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class EntryStore {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final Path baseDir = Path.of(System.getProperty("user.home"), ".timi", "entries");
  private final Path lastActionFile =
      Path.of(System.getProperty("user.home"), ".timi", "history", "last-action.json");

  private final Map<UUID, String> index = new HashMap<>();
  private final Path indexFile = baseDir.resolve("index.json");
  private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  public EntryStore() {
    loadIndex();
  }

  public Map<UUID, String> getIndex() {
    return index;
  }

  public UndoAction getLastAction() {
    try {
      return mapper.readValue(lastActionFile.toFile(), UndoAction.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void loadIndex() {
    if (Files.exists(indexFile)) {
      try {
        Map<String, String> raw = mapper.readValue(indexFile.toFile(), new TypeReference<>() {});
        raw.forEach((k, v) -> index.put(UUID.fromString(k), v));
      } catch (IOException e) {
        throw new RuntimeException("Failed to load index.json", e);
      }
    }
  }

  private void saveIndex() {
    try {
      Map<String, String> raw =
          index.entrySet().stream()
              .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
      mapper.writerWithDefaultPrettyPrinter().writeValue(indexFile.toFile(), raw);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write index.json", e);
    }
  }

  public int indexRebuild() {
    index.clear();

    for (Path file : getJsonEntryFiles()) {

      List<TimeEntry> entries = loadFromFile(file);
      String fileName = file.getFileName().toString();

      for (TimeEntry entry : entries) {
        index.put(entry.id(), fileName);
      }
    }

    saveIndex();
    return index.size();
  }

  public List<String> validateIndex() {
    List<String> issues = new ArrayList<>();
    Set<UUID> seen = new HashSet<>();
    Set<UUID> foundInFiles = new HashSet<>();

    List<Path> jsonFiles = getJsonEntryFiles();

    for (Path file : jsonFiles) {
      String expectedPrefix = file.getFileName().toString().substring(0, 7);
      List<TimeEntry> entries = loadFromFile(file);

      for (TimeEntry entry : entries) {
        UUID id = entry.id();
        foundInFiles.add(id);

        // Detect duplicate UUIDs
        if (!seen.add(id)) {
          issues.add("❌ Duplicate entry ID found: " + id);
        }

        // Validate index contains this entry
        String indexedFile = index.get(id);
        if (indexedFile == null) {
          issues.add("❌ Missing index entry for ID: " + id);
        } else if (!indexedFile.equals(file.getFileName().toString())) {
          issues.add(
              String.format(
                  "❌ Incorrect index mapping for ID %s: expected %s, found %s",
                  id, file.getFileName(), indexedFile));
        }

        // Validate correct file based on startTime
        String actualPrefix = entry.startTime().toLocalDate().toString().substring(0, 7);
        if (!expectedPrefix.equals(actualPrefix)) {
          issues.add(
              String.format(
                  "⚠️ Entry %s appears misfiled: file %s vs. entry date %s",
                  id, expectedPrefix, actualPrefix));
        }
      }
    }

    // Detect orphaned index entries (point to non-existent UUIDs)
    for (UUID id : index.keySet()) {
      if (!foundInFiles.contains(id)) {
        issues.add("⚠️ Index references missing entry ID: " + id);
      }
    }

    return issues;
  }

  public Optional<TimeEntry> findById(UUID id) {
    String fileName = index.get(id);
    if (fileName == null) {
      return Optional.empty();
    }
    Path file = baseDir.resolve(fileName);

    List<TimeEntry> entries = loadFromFile(file);
    return entries.stream().filter(e -> e.id().equals(id)).findFirst();
  }

  public void saveEntry(TimeEntry entry) {
    try {
      Files.createDirectories(baseDir);
      Path file = resolveFileFor(entry.startTime());

      List<TimeEntry> entries = new ArrayList<>();
      if (Files.exists(file)) {
        entries.addAll(Arrays.asList(mapper.readValue(file.toFile(), TimeEntry[].class)));
      }
      entries.add(entry);
      mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), entries);
      index.put(entry.id(), file.getFileName().toString());
      saveIndex();
      recordUndo("add", null, entry);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save entry", e);
    }
  }

  public boolean updateFullEntry(
      UUID id,
      LocalDateTime newStart,
      Integer newDuration,
      String newNote,
      String newActivityType,
      Set<String> newTags,
      Set<String> newMetaTags) {

    String currentFileName = index.get(id);
    if (currentFileName == null) {
      return false;
    }

    Path currentFile = baseDir.resolve(currentFileName);
    List<TimeEntry> currentEntries = loadFromFile(currentFile);

    TimeEntry existing =
        currentEntries.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);

    if (existing == null) {
      return false;
    }

    // Build updated entry
    TimeEntry updatedEntry =
        new TimeEntry(
            id,
            newStart != null ? newStart : existing.startTime(),
            newDuration != null ? newDuration : existing.durationMinutes(),
            newNote != null ? newNote : existing.note(),
            newActivityType != null ? newActivityType : existing.activityType(),
            newTags != null ? newTags : existing.tags(),
            newMetaTags != null ? newMetaTags : existing.metaTags());

    Path newFile = resolveFileFor(updatedEntry.startTime());

    // If the file is unchanged, just replace it in place
    if (newFile.equals(currentFile)) {
      List<TimeEntry> updated =
          currentEntries.stream().map(e -> e.id().equals(id) ? updatedEntry : e).toList();
      saveToFile(currentFile, updated);
      recordUndo("edit", existing, updatedEntry);
      return true;
    }

    // Otherwise: remove from current file, add to new file
    currentEntries.removeIf(e -> e.id().equals(id));
    saveToFile(currentFile, currentEntries);

    List<TimeEntry> newFileEntries = loadFromFile(newFile);
    newFileEntries.add(updatedEntry);
    saveToFile(newFile, newFileEntries);

    // Update index
    index.put(id, newFile.getFileName().toString());
    saveIndex();

    return true;
  }

  public boolean deleteById(UUID id) {
    String fileName = index.get(id);
    if (fileName == null) {
      return false;
    }

    Path file = baseDir.resolve(fileName);
    List<TimeEntry> entries = loadFromFile(file);
    Optional<TimeEntry> toRemove = entries.stream().filter(e -> e.id().equals(id)).findFirst();
    if (toRemove.isPresent()) {
      entries.remove(toRemove.get());
      saveToFile(file, entries);
      index.remove(id);
      saveIndex();
      recordUndo("delete", toRemove.get(), null);
    }
    return toRemove.isPresent();
  }

  public List<TimeEntry> loadFromFile(Path file) {
    try {
      if (!Files.exists(file)) {
        return new ArrayList<>();
      }
      return new ArrayList<>(Arrays.asList(mapper.readValue(file.toFile(), TimeEntry[].class)));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load from file: " + file, e);
    }
  }

  public void saveToFile(Path file, List<TimeEntry> entries) {
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), entries);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save to file: " + file, e);
    }
  }

  public List<TimeEntry> loadAllEntries(String month) {
    List<TimeEntry> all = new ArrayList<>();
    if (!Files.exists(baseDir)) {
      return all;
    }

    List<Path> files = getJsonEntryFiles();
    List<Path> filtered =
        (month != null)
            ? files.stream().filter(f -> f.getFileName().toString().startsWith(month)).toList()
            : files;

    for (Path path : filtered) {
      all.addAll(loadFromFile(path));
    }

    return all;
  }

  private Path resolveFileFor(LocalDateTime dateTime) {
    String fileName = dateTime.toLocalDate().format(FORMATTER) + ".json";
    return baseDir.resolve(fileName);
  }

  private List<Path> getJsonEntryFiles() {
    try (Stream<Path> files = Files.list(baseDir)) {
      return files
          .filter(p -> p.getFileName().toString().endsWith(".json"))
          .filter(p -> !p.getFileName().toString().equals("index.json"))
          .toList();
    } catch (IOException e) {
      throw new RuntimeException("Failed to list entry files in " + baseDir, e);
    }
  }

  private void recordUndo(String action, TimeEntry entryBefore, TimeEntry entryAfter) {
    try {
      Path historyDir = Path.of(System.getProperty("user.home"), ".timi", "history");
      Files.createDirectories(historyDir);
      Path file = historyDir.resolve("last-action.json");

      UndoAction undo = new UndoAction(action, LocalDateTime.now(), entryBefore, entryAfter);

      mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), undo);
    } catch (IOException e) {
      System.err.println("❌ Failed to record undo action: " + e.getMessage());
    }
  }
}
