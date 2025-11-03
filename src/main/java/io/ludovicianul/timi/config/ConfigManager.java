package io.ludovicianul.timi.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ludovicianul.timi.persistence.EntryStore;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

@Singleton
public class ConfigManager {
  private final Path configPath = Path.of(System.getProperty("user.home"), ".timi", "config.json");
  private final ObjectMapper mapper = new ObjectMapper();

  private final ConfigData config;

  @Inject EntryStore entryStore;

  public ConfigManager() {
    try {
      if (Files.exists(configPath)) {
        config = mapper.readValue(configPath.toFile(), ConfigData.class);
      } else {
        config = new ConfigData();
        config.tags.add("general");
        config.types.addAll(Set.of("work", "meeting", "prep"));
        config.metaTags.addAll(Set.of("ai", "boring", "urgent"));
        save();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to load config", e);
    }
  }

  public boolean addTag(String tag) {
    if (!config.tags.contains(tag)) {
      config.tags.add(tag);
      save();
      return true;
    }
    return false;
  }

  public boolean addActivityType(String type) {
    if (!config.types.contains(type)) {
      config.types.add(type);
      save();
      return true;
    }
    return false;
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

    boolean removed = config.tags.remove(tag);
    if (removed) {
      save();
    }
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

    boolean removed = config.types.remove(type);
    if (removed) {
      save();
    }
    return removed;
  }

  public void save() {
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), config);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save config", e);
    }
  }

  public Set<String> getTags() {
    return config.tags;
  }

  public Set<String> getActivityTypes() {
    return config.types;
  }

  public Set<String> getMetaTags() {
    return config.metaTags;
  }

  public boolean isNotValidMetaTag(String tag) {
    return !config.metaTags.contains(tag.toLowerCase());
  }

  public boolean isNotValidTag(String tag) {
    return !config.tags.contains(tag.toLowerCase());
  }

  public boolean isNotValidActivity(String activityType) {
    return !config.types.contains(activityType.toLowerCase());
  }

  public boolean isGitEnabled() {
    return config.gitEnabled;
  }

  public void setGitEnabled(boolean enabled) {
    config.gitEnabled = enabled;
    save();
  }

  public int getDeepWorkValue() {
    return config.deepWorkValue;
  }

  public void setDeepWorkValue(int value) {
    config.deepWorkValue = value;
    save();
  }

  public String getZenStyle() {
    return config.zenStyle;
  }

  public void setZenStyle(String zenStyle) {
    config.zenStyle = zenStyle;
    save();
  }

  public int getFocusedWorkValue() {
    return config.focusedWorkValue;
  }

  public void setFocusedWorkValue(int value) {
    config.focusedWorkValue = value;
    save();
  }

  public void setColorOutput(boolean enabled) {
    config.colorOutput = enabled;
    save();
  }

  public boolean isColorOutput() {
    return config.colorOutput;
  }

  public int getRoundSessionMinutes() {
    return config.roundSessionMinutes;
  }

  public void setRoundSessionMinutes(int value) {
    if (value < 0 || value > 10) {
      throw new IllegalArgumentException("Round session minutes must be between 0 and 10.");
    }
    config.roundSessionMinutes = value;
    save();
  }

  public int getShortDurationThreshold() {
    return config.shortDurationThreshold;
  }

  public void setShortDurationThreshold(int value) {
    config.shortDurationThreshold = value;
    save();
  }

  public boolean addMetaTag(String normalizedName) {
    if (!config.metaTags.contains(normalizedName)) {
      config.metaTags.add(normalizedName);
      save();
      return true;
    }
    return false;
  }

  @RegisterForReflection
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ConfigData {
    public Set<String> tags = new HashSet<>();
    public Set<String> types = new HashSet<>();
    public Set<String> metaTags = new HashSet<>();
    public boolean gitEnabled = true;
    public boolean colorOutput = false;
    public int deepWorkValue = 2;
    public int focusedWorkValue = 3;
    public int shortDurationThreshold = 10;
    public int roundSessionMinutes = 0; // 0, 5, 10
    public String zenStyle = "zen"; // coach, zen, snarky
  }
}
