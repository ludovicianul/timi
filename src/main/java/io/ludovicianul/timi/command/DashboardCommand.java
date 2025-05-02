package io.ludovicianul.timi.command;

import static io.ludovicianul.timi.util.Utils.formatMinutes;

import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import picocli.CommandLine.*;

@Command(
    name = "dashboard",
    description = "Show a daily or monthly dashboard",
    mixinStandardHelpOptions = true)
public class DashboardCommand implements Runnable {

  @Option(names = "--day", description = "Specific day to display (yyyy-MM-dd)")
  LocalDate day;

  @Option(names = "--month", description = "Specific month to display (yyyy-MM)")
  String month;

  @Inject EntryStore entryStore;
  @Inject ConfigManager configManager;

  @Override
  public void run() {
    LocalDate today = LocalDate.now();
    LocalDate selectedDay = day != null ? day : today;

    List<TimeEntry> entries = loadEntries(selectedDay);

    if (entries.isEmpty()) {
      System.out.println("📭 No entries found for the selected period.");
      return;
    }

    System.out.println("\n📋 Timi Dashboard");
    System.out.println("=".repeat(40));

    if (day != null || (month == null)) {
      System.out.printf("Today - %s%n", selectedDay);
    } else {
      System.out.printf("Month - %s%n", month);
    }

    int totalMinutes = entries.stream().mapToInt(TimeEntry::durationMinutes).sum();
    System.out.printf("\n🕒 Total Logged: %s%n", formatMinutes(totalMinutes));

    Map<LocalDate, Set<String>> dayTypes = new HashMap<>();
    for (TimeEntry e : entries) {
      LocalDate date = e.startTime().toLocalDate();
      dayTypes.putIfAbsent(date, new HashSet<>());
      dayTypes.get(date).add(e.activityType().toLowerCase());
    }

    long deepDays =
        dayTypes.values().stream()
            .filter(types -> types.size() <= configManager.getDeepWorkValue())
            .count();
    long focusedDays =
        dayTypes.values().stream()
            .filter(
                types ->
                    types.size() > configManager.getDeepWorkValue()
                        && types.size() <= configManager.getFocusedWorkValue())
            .count();
    long contextSwitchDays = dayTypes.size() - deepDays - focusedDays;

    System.out.printf("📈 Deep Focus Days: %d%n", deepDays);
    System.out.printf("🟡 Focused Days: %d%n", focusedDays);
    System.out.printf("🔴 Context Switching Days: %d%n", contextSwitchDays);

    System.out.printf("⚙️ Sessions Completed: %d%n", entries.size());

    Map<String, Long> typeCounts =
        entries.stream()
            .collect(Collectors.groupingBy(TimeEntry::activityType, Collectors.counting()));

    Map<String, Long> tagCounts =
        entries.stream()
            .flatMap(e -> e.tags().stream())
            .collect(Collectors.groupingBy(String::toLowerCase, Collectors.counting()));

    String topType =
        typeCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("-");

    String topTag =
        tagCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("-");

    System.out.printf("📎 Most Used Type: %s%n", topType);
    System.out.printf("🏷️ Most Used Tag: %s%n", topTag);

    System.out.println("\n-----------------------------------------");

    if (month == null) { // Show timeline only for day view
      System.out.println("📊 Today's Timeline:");
      entries.stream()
          .sorted(Comparator.comparing(TimeEntry::startTime))
          .forEach(
              e -> {
                String start = e.startTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                int barLength = Math.max(1, e.durationMinutes() / 10);
                String bar = "█".repeat(barLength);
                System.out.printf(
                    "  %s  %-15s %s (%s)%n",
                    start, bar, e.activityType(), formatMinutes(e.durationMinutes()));
              });
      System.out.println("\n-----------------------------------------");
    }

    System.out.println("📋 Quick Audit:");
    long shortDurations = entries.stream().filter(e -> e.durationMinutes() < 10).count();
    long emptyNotes = entries.stream().filter(e -> e.note() == null || e.note().isBlank()).count();

    System.out.printf("• Short entries (<10m): %d%n", shortDurations);
    System.out.printf("• Empty notes: %d%s%n", emptyNotes, emptyNotes > 0 ? " ⚠️" : "");

    System.out.println("\n" + "=".repeat(40));

    if (deepDays >= focusedDays && deepDays >= contextSwitchDays) {
      System.out.println("✅ Excellent deep work! 🧠");
    } else if (focusedDays >= deepDays && focusedDays >= contextSwitchDays) {
      System.out.println("🟡 Good focus, room to improve! ⚡");
    } else {
      System.out.println("🔴 High context switching! 🌀");
    }
  }

  private List<TimeEntry> loadEntries(LocalDate day) {
    Set<String> months = new HashSet<>();
    months.add(Objects.requireNonNullElseGet(month, () -> day.toString().substring(0, 7)));

    return months.stream()
        .flatMap(m -> entryStore.loadAllEntries(m).stream())
        .filter(
            e -> {
              if (month != null) {
                return e.startTime().getMonthValue() == Integer.parseInt(month.substring(5))
                    && e.startTime().getYear() == Integer.parseInt(month.substring(0, 4));
              } else {
                return e.startTime().toLocalDate().equals(day);
              }
            })
        .toList();
  }
}
