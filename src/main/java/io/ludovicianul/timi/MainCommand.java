package io.ludovicianul.timi;

import io.ludovicianul.timi.command.AddCommand;
import io.ludovicianul.timi.command.AnalyzeCommand;
import io.ludovicianul.timi.command.ConfigCommand;
import io.ludovicianul.timi.command.DeleteCommand;
import io.ludovicianul.timi.command.EditCommand;
import io.ludovicianul.timi.command.InfoCommand;
import io.ludovicianul.timi.command.ListCommand;
import io.ludovicianul.timi.command.NotesCommand;
import io.ludovicianul.timi.command.SearchCommand;
import io.ludovicianul.timi.command.StatsCommand;
import io.ludovicianul.timi.version.VersionProvider;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.AutoComplete;
import picocli.CommandLine;

@CommandLine.Command(
    name = "timi",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    subcommands = {
      AddCommand.class,
      ConfigCommand.class,
      DeleteCommand.class,
      EditCommand.class,
      ListCommand.class,
      StatsCommand.class,
      NotesCommand.class,
      SearchCommand.class,
      AnalyzeCommand.class,
      InfoCommand.class,
      AutoComplete.GenerateCompletion.class
    },
    description = "Time unit tracker for daily activities")
@TopCommand
public class MainCommand implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(MainCommand.class);

  @Override
  public void run() {
    logger.debug("Starting timi");
    System.out.println("Use `timi add` to log time.");
  }
}
