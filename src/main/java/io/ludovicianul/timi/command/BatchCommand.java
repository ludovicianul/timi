package io.ludovicianul.timi.command;

import io.ludovicianul.timi.git.GitManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import io.ludovicianul.timi.util.Utils;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(
    name = "batch",
    description = "Perform batch operations on time entries",
    subcommands = {BatchCommand.BatchAdd.class, BatchCommand.BatchDelete.class})
public class BatchCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("Use a subcommand: batch add or batch delete");
  }

  @CommandLine.Command(
      name = "add",
      description =
          "Batch add time entries from a CSV file. CSV format: startTime,duration,activityType,tags,note")
  public static class BatchAdd implements Runnable {
    @Option(names = "--file", required = true, description = "Path to the CSV file")
    String filePath;

    @Inject EntryStore entryStore;
    @Inject GitManager gitManager;

    @Override
    public void run() {
      System.out.println(" ");
      try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("startTime") || line.isBlank()) {
            continue;
          }
          String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

          for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replaceAll("^\"|\"$", "");
          }

          if (parts.length < 6) {
            System.err.println("❌ Invalid line: " + line);
            continue;
          }

          LocalDateTime startTime = Utils.parseDateTime(parts[0]);
          int duration = Integer.parseInt(parts[1].trim());
          String type = parts[2].trim().toLowerCase();
          Set<String> tags =
              Arrays.stream(parts[3].split(";"))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .collect(Collectors.toSet());
          Set<String> metaTags =
              Arrays.stream(parts[4].split(";"))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .collect(Collectors.toSet());
          String note = parts[5].trim();
          TimeEntry entry =
              new TimeEntry(UUID.randomUUID(), startTime, duration, note, type, tags, metaTags);
          entryStore.saveEntry(entry);
          gitManager.commit("Batch added entry on " + startTime.format(DateTimeFormatter.ISO_DATE));
          System.out.println("✅ Added entry: " + entry.id());
        }
        System.out.println("✅ Batch add completed.");
      } catch (IOException e) {
        System.err.println("❌ Failed to read file: " + e.getMessage());
      } catch (Exception e) {
        System.err.println("❌ Error processing batch add: " + e.getMessage());
      }
    }
  }

  @CommandLine.Command(
      name = "delete",
      description =
          "Batch delete time entries by IDs from a file. Each line should contain an entry ID.")
  public static class BatchDelete implements Runnable {
    @Option(
        names = "--file",
        required = true,
        description = "Path to the file containing entry IDs")
    String filePath;

    @Inject EntryStore entryStore;
    @Inject GitManager gitManager;

    @Override
    public void run() {
      System.out.println(" ");
      try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        String line;
        int successCount = 0, failureCount = 0;
        while ((line = reader.readLine()) != null) {
          String id = line.trim();
          if (id.isEmpty()) {
            continue;
          }
          boolean deleted = entryStore.deleteById(UUID.fromString(id));
          if (deleted) {
            gitManager.commit("Batch deleted entry " + id);
            successCount++;
            System.out.println("✅ Deleted entry: " + id);
          } else {
            failureCount++;
            System.err.println("❌ Failed to delete entry: " + id);
          }
        }
        System.out.printf(
            "Batch delete completed. Success: %d, Failed: %d%n", successCount, failureCount);
      } catch (IOException e) {
        System.err.println("❌ Failed to read file: " + e.getMessage());
      } catch (Exception e) {
        System.err.println("❌ Error processing batch delete: " + e.getMessage());
      }
    }
  }
}
