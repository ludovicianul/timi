package io.ludovicianul.timi.command;

import io.ludovicianul.timi.command.config.AddTagCommand;
import io.ludovicianul.timi.command.config.AddTypeCommand;
import io.ludovicianul.timi.command.config.ListConfigCommand;
import io.ludovicianul.timi.command.config.PruneConfigCommand;
import io.ludovicianul.timi.command.config.RemoveTagCommand;
import io.ludovicianul.timi.command.config.RemoveTypeCommand;
import io.ludovicianul.timi.command.config.SetSettingCommand;
import io.ludovicianul.timi.command.config.ShowTagsCommand;
import io.ludovicianul.timi.command.config.ShowTypesCommand;
import io.ludovicianul.timi.command.config.ValidateConfigCommand;
import picocli.CommandLine;

@CommandLine.Command(
    name = "config",
    description = "View or modify configuration",
    mixinStandardHelpOptions = true,
    subcommands = {
      AddTagCommand.class,
      AddTypeCommand.class,
      PruneConfigCommand.class,
      RemoveTypeCommand.class,
      RemoveTagCommand.class,
      ShowTagsCommand.class,
      ShowTypesCommand.class,
      ValidateConfigCommand.class,
      ListConfigCommand.class,
      SetSettingCommand.class
    })
public class ConfigCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("Use subcommands: add-tag, add-type, show-tags, show-types");
  }
}
