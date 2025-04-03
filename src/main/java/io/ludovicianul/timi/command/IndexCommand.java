package io.ludovicianul.timi.command;

import io.ludovicianul.timi.persistence.EntryStore;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(
    name = "index",
    description = "Manage the entry index",
    subcommands = {
      IndexCommand.Rebuild.class,
      IndexCommand.Validate.class,
      IndexCommand.Show.class
    })
public class IndexCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("Use one of the subcommands: rebuild, validate, show");
  }

  @CommandLine.Command(name = "rebuild", description = "Rebuild the index from scratch")
  public static class Rebuild implements Runnable {
    @Inject EntryStore entryStore;

    @Override
    public void run() {
      int indexed = entryStore.indexRebuild();
      System.out.printf("✅ Index rebuilt with %d entries.%n", indexed);
    }
  }

  @CommandLine.Command(name = "validate", description = "Validate index consistency")
  public static class Validate implements Runnable {
    @Inject EntryStore entryStore;

    @Override
    public void run() {
      var issues = entryStore.validateIndex();
      if (issues.isEmpty()) {
        System.out.println("✅ Index is consistent with stored entries.");
      } else {
        System.out.printf("❌ Found %d issue(s):%n", issues.size());
        issues.forEach(System.out::println);
      }
    }
  }

  @CommandLine.Command(name = "show", description = "Show current index entries")
  public static class Show implements Runnable {
    @Inject EntryStore entryStore;

    @Override
    public void run() {
      entryStore.getIndex().forEach((uuid, file) -> System.out.printf("• %s → %s%n", uuid, file));
    }
  }
}
