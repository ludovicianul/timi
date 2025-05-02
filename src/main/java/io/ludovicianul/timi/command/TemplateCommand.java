package io.ludovicianul.timi.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ludovicianul.timi.config.ConfigManager;
import io.ludovicianul.timi.persistence.EntryStore;
import io.ludovicianul.timi.persistence.TimeEntry;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import picocli.CommandLine.*;

@Command(
    name = "template",
    description = "Manage entry templates",
    mixinStandardHelpOptions = true,
    subcommands = {
      TemplateCommand.Save.class,
      TemplateCommand.Use.class,
      TemplateCommand.ListTemplates.class,
      TemplateCommand.Delete.class
    })
public class TemplateCommand implements Runnable {

  @Override
  public void run() {
    System.out.println(
        "Use 'timi template save', 'timi template use', 'timi template list', or 'timi template delete'.");
  }

  @Command(name = "save", description = "Save a new template")
  public static class Save implements Runnable {

    @Parameters(index = "0", description = "Template name")
    String name;

    @Option(names = "--type", required = true, description = "Activity type")
    String type;

    @Option(names = "--tags", description = "Comma-separated list of tags")
    String tags;

    @Option(names = "--duration", required = true, description = "Duration in minutes")
    int duration;

    @Option(names = "--note", description = "Optional note for the template")
    String note;

    private final Path templatesDir =
        Path.of(System.getProperty("user.home"), ".timi", "templates");

    @Inject ConfigManager configManager;

    @Override
    public void run() {
      try {
        Files.createDirectories(templatesDir);

        if (configManager.isNotValidActivity(type.toLowerCase())) {
          System.out.printf("❌ Invalid activity type: %s%n", type);
          return;
        }

        List<String> tagList =
            (tags != null) ? Arrays.stream(tags.split(",")).map(String::trim).toList() : List.of();
        for (String tag : tagList) {
          if (configManager.isNotValidTag(tag.toLowerCase())) {
            System.out.printf("❌ Invalid tag: %s%n", tag);
            return;
          }
        }

        Path templateFile = templatesDir.resolve(name + ".json");

        Map<String, Object> template = new HashMap<>();
        template.put("type", type);
        template.put("tags", tagList);
        template.put("duration", duration);
        if (note != null && !note.isBlank()) {
          template.put("note", note);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(templateFile.toFile(), template);

        System.out.printf("✅ Template '%s' saved successfully.%n", name);
      } catch (IOException e) {
        System.out.println("❌ Failed to save template: " + e.getMessage());
      }
    }
  }

  @Command(name = "use", description = "Use a saved template to add an entry")
  public static class Use implements Runnable {

    @Parameters(index = "0", description = "Template name")
    String name;

    @Option(names = "--note", description = "Optional note for the entry")
    String note;

    private final Path templatesDir =
        Path.of(System.getProperty("user.home"), ".timi", "templates");

    @Inject EntryStore entryStore;

    @Override
    public void run() {
      try {
        Path templateFile = templatesDir.resolve(name + ".json");

        if (!Files.exists(templateFile)) {
          System.out.printf("❌ Template '%s' not found.%n", name);
          return;
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> template = mapper.readValue(templateFile.toFile(), Map.class);

        String type = (String) template.get("type");
        List<String> tags = (List<String>) template.get("tags");
        int duration = (int) template.get("duration");
        String templateNote = (String) template.get("note");

        if ((note == null || note.isBlank()) && (templateNote == null || templateNote.isBlank())) {
          System.out.println("❌ No note provided.");
          return;
        }
        if (templateNote != null && !templateNote.isBlank()) {
          note = templateNote;
        }

        TimeEntry entry =
            new TimeEntry(
                UUID.randomUUID(),
                LocalDate.now().atStartOfDay(),
                duration,
                note,
                type,
                new HashSet<>(tags),
                Set.of());

        entryStore.saveEntry(entry);

        System.out.printf("✅ %s entry added using template '%s'.%n", entry.id(), name);
      } catch (IOException e) {
        System.out.println("❌ Failed to use template: " + e.getMessage());
      }
    }
  }

  @Command(name = "list", description = "List all saved templates")
  public static class ListTemplates implements Runnable {

    private final Path templatesDir =
        Path.of(System.getProperty("user.home"), ".timi", "templates");

    @Override
    public void run() {
      if (!Files.exists(templatesDir)) {
        System.out.println("📭 No templates found.");
        return;
      }
      try (var paths = Files.list(templatesDir)) {

        ObjectMapper mapper = new ObjectMapper();

        System.out.println("\n📄 Saved Templates:");
        System.out.println("=".repeat(80));

        paths
            .filter(p -> p.toString().endsWith(".json"))
            .forEach(
                p -> {
                  try {
                    Map<String, Object> template = mapper.readValue(p.toFile(), Map.class);
                    String name = p.getFileName().toString().replace(".json", "");
                    String type = (String) template.get("type");
                    List<String> tags = (List<String>) template.get("tags");
                    int duration = (int) template.get("duration");
                    String note = (String) template.get("note");

                    System.out.printf(
                        "• %s → Type: %s | Tags: %s | Duration: %dmin | Note: %s%n",
                        name,
                        type,
                        String.join(", ", tags),
                        duration,
                        note != null ? note : "<none>");
                  } catch (IOException e) {
                    System.out.println("❌ Failed to read template: " + p.getFileName());
                  }
                });

        System.out.println("=".repeat(80));

      } catch (IOException e) {
        System.out.println("❌ Failed to list templates: " + e.getMessage());
      }
    }
  }

  @Command(name = "delete", description = "Delete a saved template")
  public static class Delete implements Runnable {

    @Parameters(index = "0", description = "Template name")
    String name;

    private final Path templatesDir =
        Path.of(System.getProperty("user.home"), ".timi", "templates");

    @Override
    public void run() {
      try {
        Path templateFile = templatesDir.resolve(name + ".json");

        if (!Files.exists(templateFile)) {
          System.out.printf("❌ Template '%s' not found.%n", name);
          return;
        }

        System.out.printf("Are you sure you want to delete template '%s'? (y/N): ", name);
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine().trim().toLowerCase();
        if (!input.equals("y")) {
          System.out.println("Deletion cancelled.");
          return;
        }
        Files.delete(templateFile);
        System.out.printf("✅ Template '%s' deleted successfully.%n", name);

      } catch (IOException e) {
        System.out.println("❌ Failed to delete template: " + e.getMessage());
      }
    }
  }
}
