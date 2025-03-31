package io.ludovicianul.timi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ludovicianul.timi.persistence.EntryStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

@Singleton
public class ConfigManager {
  private Set<String> activityTypes = Set.of();
  private Set<String> tags = Set.of();
  private final Path configPath = Path.of(System.getProperty("user.home"), ".timi", "config.json");

  @Inject EntryStore entryStore;

  public ConfigManager() {
    load();
  }

  public void load() {
    try {
      if (!Files.exists(configPath)) {
        Files.createDirectories(configPath.getParent());
        Files.writeString(
            configPath,
            """
                {
                  "activityTypes": ["work", "meeting", "prep"],
                  "tags": ["general"]
                }""");
      }
      var json = new ObjectMapper().readTree(configPath.toFile());
      activityTypes = new HashSet<>();
      tags = new HashSet<>();
      json.get("activityTypes")
          .forEach(v -> activityTypes.add(v.asText().toLowerCase(Locale.ROOT)));
      json.get("tags").forEach(v -> tags.add(v.asText().toLowerCase(Locale.ROOT)));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load config.json", e);
    }
  }

  public boolean addTag(String tag) {
    if (!tags.contains(tag)) {
      tags.add(tag);
      save();
      return true;
    }
    return false;
  }

  public boolean addActivityType(String type) {
    if (!activityTypes.contains(type)) {
      activityTypes.add(type);
      save();
      return true;
    }
    return false;
  }

  private void save() {
    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    ObjectNode root = mapper.createObjectNode();
    root.putPOJO("activityTypes", activityTypes);
    root.putPOJO("tags", tags);
    try {
      mapper.writeValue(configPath.toFile(), root);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save config", e);
    }
  }

  public boolean removeTag(String tag) {
    boolean used =
        entryStore.loadAllEntries(null).stream()
            .flatMap(e -> e.tags().stream())
            .anyMatch(t -> t.equalsIgnoreCase(tag));

    if (used) {
      System.out.printf("⚠️ Tag '%s' is in use by existing entries. Remove anyway? (y/N): ", tag);
      Scanner scanner = new Scanner(System.in);
      if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
        System.out.println("❌ Removal cancelled.");
        return false;
      }
    }

    boolean removed = tags.remove(tag);
    if (removed) save();
    return removed;
  }

  public boolean removeActivityType(String type) {
    boolean used =
        entryStore.loadAllEntries(null).stream()
            .anyMatch(e -> e.activityType().equalsIgnoreCase(type));
    if (used) {
      System.out.printf(
          "⚠️ Activity type '%s' is in use by existing entries. Remove anyway? (y/N): ", type);
      Scanner scanner = new Scanner(System.in);
      if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
        System.out.println("❌ Removal cancelled.");
        return false;
      }
    }

    boolean removed = activityTypes.remove(type);
    if (removed) save();
    return removed;
  }

  public boolean isNotValidActivity(String value) {
    return !activityTypes.contains(value);
  }

  public boolean isNotValidTag(String tag) {
    return !tags.contains(tag);
  }

  public Set<String> getActivityTypes() {
    return activityTypes;
  }

  public Set<String> getTags() {
    return tags;
  }
}
