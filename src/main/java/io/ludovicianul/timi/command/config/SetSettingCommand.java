package io.ludovicianul.timi.command.config;

import static io.ludovicianul.timi.command.config.SetSettingCommand.Settings.colorOutput;
import static io.ludovicianul.timi.command.config.SetSettingCommand.Settings.deepWorkValue;
import static io.ludovicianul.timi.command.config.SetSettingCommand.Settings.focusedWorkValue;
import static io.ludovicianul.timi.command.config.SetSettingCommand.Settings.gitEnabled;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "set",
    description = "Set advanced settings (e.g., gitEnabled, deepWorkValue)",
    mixinStandardHelpOptions = true)
public class SetSettingCommand implements Runnable {

  @Parameters(index = "0", description = "Setting key (e.g., gitEnabled, deepWorkValue)")
  Settings key;

  @Parameters(index = "1", description = "Setting value")
  String value;

  @Inject ConfigManager configManager;

  enum Settings {
    gitEnabled,
    deepWorkValue,
    focusedWorkValue,
    colorOutput
  }

  @Override
  public void run() {
    switch (key) {
      case gitEnabled -> setBoolean(value, configManager::setGitEnabled, gitEnabled.name());
      case colorOutput -> setBoolean(value, configManager::setColorOutput, colorOutput.name());
      case deepWorkValue ->
          setInt(value, v -> configManager.setDeepWorkValue(v), deepWorkValue.name());
      case focusedWorkValue ->
          setInt(value, v -> configManager.setFocusedWorkValue(v), focusedWorkValue.name());
    }
  }

  private void setBoolean(String value, java.util.function.Consumer<Boolean> setter, String label) {
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
      boolean parsed = Boolean.parseBoolean(value);
      setter.accept(parsed);
      System.out.printf("✅ %s set to %s%n", label, parsed);
    } else {
      System.err.printf("❌ Invalid value for %s. Use true or false.%n", label);
    }
  }

  private void setInt(String value, java.util.function.IntConsumer setter, String label) {
    try {
      int v = Integer.parseInt(value);
      setter.accept(v);
      System.out.printf("✅ %s set to %d%n", label, v);
    } catch (NumberFormatException e) {
      System.err.printf("❌ Invalid number for %s: %s%n", label, value);
    }
  }
}
