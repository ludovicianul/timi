package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.formatMinutes;

import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import io.ludovicianul.timi.persistence.zen.ZenSuggestionsStore;
import jakarta.inject.Inject;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import picocli.CommandLine.*;

@Command(
    name = "zen",
    description = "Reflect on your focus and balance",
    mixinStandardHelpOptions = true,
    subcommands = ZenCommand.Report.class)
public class ZenCommand implements Runnable {

  @Option(names = "--day", description = "Day to reflect on (default: today)")
  LocalDate day;

  @Inject EntryStore entryStore;
  @Inject ConfigManager configManager;
  @Inject ZenSuggestionsStore suggestionBank;

  enum Mood {
    CALM,
    SCATTERED,
    INTENTIONAL,
    LIGHT,
    UNKNOWN
  }

  @Override
  public void run() {
    LocalDate selected = day != null ? day : LocalDate.now();
    List<TimeEntry> entries =
        entryStore.loadAllEntries(selected.toString().substring(0, 7)).stream()
            .filter(e -> e.startTime().toLocalDate().equals(selected))
            .toList();

    if (entries.isEmpty()) {
      System.out.printf("\n🧘‍♂️ Timi Zen (%s)%n", selected);
      System.out.println("📭 No entries found for this day. Stillness is also a rhythm.");
      return;
    }

    System.out.printf("\n🧘‍♂️ Timi Zen (%s)%n", selected);
    System.out.println("=".repeat(50));

    int totalMinutes = entries.stream().mapToInt(TimeEntry::durationMinutes).sum();
    int sessionCount = entries.size();

    Map<String, Set<String>> dayTypes = new HashMap<>();
    for (TimeEntry e : entries) {
      LocalDate d = e.startTime().toLocalDate();
      dayTypes.putIfAbsent(d.toString(), new HashSet<>());
      dayTypes.get(d.toString()).add(e.activityType().toLowerCase());
    }

    Set<String> allTypes =
        dayTypes.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    int typeCount = allTypes.size();

    Mood mood = determineMood(typeCount);

    System.out.printf(
        "🕒 You logged %s across %d sessions.%n", formatMinutes(totalMinutes), sessionCount);
    System.out.printf("📈 Focus Profile: %s%n", moodName(mood));

    Map<String, Long> typeUsage =
        entries.stream()
            .collect(Collectors.groupingBy(TimeEntry::activityType, Collectors.counting()));

    String mostUsedType =
        typeUsage.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("-");

    System.out.printf("💡 Most time spent on: %s%n", mostUsedType);
    System.out.println();

    String persona = configManager.getZenStyle();
    Map<String, String> context =
        Map.of(
            "duration", formatMinutes(totalMinutes),
            "sessions", String.valueOf(sessionCount),
            "topType", mostUsedType,
            "day", selected.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));

    String message = suggestionBank.pick(mood.name(), persona, context);
    printValued(message);

    System.out.println("=".repeat(50));
  }

  private Mood determineMood(int typeCount) {
    if (typeCount <= configManager.getDeepWorkValue()) return Mood.CALM;
    if (typeCount <= configManager.getFocusedWorkValue()) return Mood.INTENTIONAL;
    if (typeCount > configManager.getFocusedWorkValue()) return Mood.SCATTERED;
    return Mood.UNKNOWN;
  }

  private String moodName(Mood mood) {
    return switch (mood) {
      case CALM -> "Deep Work 🧠";
      case INTENTIONAL -> "Focused ⚡";
      case SCATTERED -> "Context Switching 🌀";
      case LIGHT -> "Light Day 🌙";
      case UNKNOWN -> "Unknown";
    };
  }

  private void printValued(String message) {
    System.out.println(message);
  }

  @Command(name = "report", description = "Zen overview for the past week")
  public static class Report implements Runnable {

    @Inject EntryStore entryStore;
    @Inject ConfigManager configManager;

    @Override
    public void run() {
      LocalDate today = LocalDate.now();
      LocalDate start = today.with(DayOfWeek.MONDAY);
      LocalDate end = start.plusDays(6);

      Map<Mood, Integer> moodCounts = new EnumMap<>(Mood.class);
      int totalMinutes = 0;

      for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
        LocalDate finalDate = date;
        List<TimeEntry> entries =
            entryStore.loadAllEntries(date.toString().substring(0, 7)).stream()
                .filter(e -> e.startTime().toLocalDate().equals(finalDate))
                .toList();

        int minutes = entries.stream().mapToInt(TimeEntry::durationMinutes).sum();
        totalMinutes += minutes;

        Set<String> types =
            entries.stream()
                .map(TimeEntry::activityType)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Mood mood;
        int typeCount = types.size();
        if (typeCount <= configManager.getDeepWorkValue()) {
          mood = Mood.CALM;
        } else if (typeCount <= configManager.getFocusedWorkValue()) {
          mood = Mood.INTENTIONAL;
        } else mood = Mood.SCATTERED;

        moodCounts.merge(mood, 1, Integer::sum);
      }

      System.out.println("\n📘 Weekly Zen Report");
      System.out.println("=".repeat(40));
      System.out.printf("Total Logged: %s%n", formatMinutes(totalMinutes));
      System.out.printf("🧠 Deep Work Days: %d%n", moodCounts.getOrDefault(Mood.CALM, 0));
      System.out.printf("⚡ Focused Days: %d%n", moodCounts.getOrDefault(Mood.INTENTIONAL, 0));
      System.out.printf(
          "🌀 Context Switching Days: %d%n", moodCounts.getOrDefault(Mood.SCATTERED, 0));
      System.out.println("=".repeat(40));
    }
  }
}
