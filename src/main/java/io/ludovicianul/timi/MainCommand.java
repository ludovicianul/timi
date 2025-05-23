package io.ludovicianul.timi;

import io.ludovicianul.timi.command.AbortCommand;
import io.ludovicianul.timi.command.AddCommand;
import io.ludovicianul.timi.command.AnalyzeCommand;
import io.ludovicianul.timi.command.AuditCommand;
import io.ludovicianul.timi.command.BatchCommand;
import io.ludovicianul.timi.command.ConfigCommand;
import io.ludovicianul.timi.command.DashboardCommand;
import io.ludovicianul.timi.command.DeleteCommand;
import io.ludovicianul.timi.command.EditCommand;
import io.ludovicianul.timi.command.ExportCommand;
import io.ludovicianul.timi.command.IndexCommand;
import io.ludovicianul.timi.command.InfoCommand;
import io.ludovicianul.timi.command.LastCommand;
import io.ludovicianul.timi.command.ListCommand;
import io.ludovicianul.timi.command.NotesCommand;
import io.ludovicianul.timi.command.PauseCommand;
import io.ludovicianul.timi.command.ResumeCommand;
import io.ludovicianul.timi.command.SearchCommand;
import io.ludovicianul.timi.command.StartCommand;
import io.ludovicianul.timi.command.StatsCommand;
import io.ludovicianul.timi.command.StatusCommand;
import io.ludovicianul.timi.command.StopCommand;
import io.ludovicianul.timi.command.TemplateCommand;
import io.ludovicianul.timi.command.TimelineCommand;
import io.ludovicianul.timi.command.UndoCommand;
import io.ludovicianul.timi.command.ZenCommand;
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
      AuditCommand.class,
      AbortCommand.class,
      ConfigCommand.class,
      DeleteCommand.class,
      DashboardCommand.class,
      EditCommand.class,
      LastCommand.class,
      ListCommand.class,
      StatsCommand.class,
      NotesCommand.class,
      SearchCommand.class,
      AnalyzeCommand.class,
      InfoCommand.class,
      IndexCommand.class,
      TimelineCommand.class,
      ExportCommand.class,
      BatchCommand.class,
      UndoCommand.class,
      TemplateCommand.class,
      ZenCommand.class,
      PauseCommand.class,
      ResumeCommand.class,
      StartCommand.class,
      StopCommand.class,
      StatusCommand.class,
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
