package io.ludovicianul.timi.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
    name = "export",
    description = "Export time entries to CSV or JSON format.",
    mixinStandardHelpOptions = true)
public class ExportCommand implements Runnable {

  @CommandLine.Option(
      names = {"--format", "-f"},
      required = true,
      description = "Export format: csv or json")
  Format format;

  @CommandLine.Option(
      names = "--from",
      description = "Filter entries starting on or after this date (format: yyyy-MM-dd)")
  LocalDate from;

  @CommandLine.Option(
      names = "--to",
      description = "Filter entries ending on or before this date (format: yyyy-MM-dd)")
  LocalDate to;

  @CommandLine.Option(
      names = {"--type", "-t"},
      description = "Filter entries by activity type")
  String activityType;

  @CommandLine.Option(
      names = {"--tags", "--tag"},
      required = false,
      description = "Filter entries that contain the specified tags (semicolon separated)",
      split = ",")
  List<String> tags = List.of();

  @CommandLine.Option(
      names = {"--output", "-o"},
      required = true,
      description = "Output file path")
  String outputPath;

  @Inject EntryStore entryStore;

  public enum Format {
    csv,
    json
  }

  @Override
  public void run() {
    List<TimeEntry> entries = entryStore.loadAllEntries(null);
    if (entries.isEmpty()) {
      System.out.println("📭 No entries to export.");
      return;
    }

    entries =
        entries.parallelStream()
            .filter(
                e -> {
                  boolean match = true;
                  if (from != null) {
                    match = !e.startTime().toLocalDate().isBefore(from);
                  }
                  if (to != null) {
                    match = match && !e.startTime().toLocalDate().isAfter(to);
                  }
                  if (activityType != null) {
                    match = match && e.activityType().equalsIgnoreCase(activityType);
                  }
                  if (!tags.isEmpty()) {
                    match = match && e.tags().containsAll(tags);
                  }
                  return match;
                })
            .toList();

    try {
      if (Format.csv == format) {
        exportCSV(entries);
      } else if (Format.json == format) {
        exportJSON(entries);
      }
    } catch (IOException e) {
      System.err.println("❌ Failed to export entries: " + e.getMessage());
    }
  }

  private void exportCSV(List<TimeEntry> entries) throws IOException {
    try (FileWriter writer = new FileWriter(outputPath)) {
      // Write header
      writer.write("ID,Start Time,Duration,Activity Type,Tags,Note\n");
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      for (TimeEntry e : entries) {
        String line =
            String.format(
                "\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\"\n",
                e.id(),
                e.startTime().format(dtf),
                e.durationMinutes(),
                e.activityType(),
                String.join(";", e.tags()),
                e.note().replace("\"", "\"\""));
        writer.write(line);
      }
      System.out.println("✅ Exported entries to CSV: " + outputPath);
    }
  }

  private void exportJSON(List<TimeEntry> entries) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    try (FileWriter writer = new FileWriter(outputPath)) {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, entries);
      System.out.println("✅ Exported entries to JSON: " + outputPath);
    }
  }
}
