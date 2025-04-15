package io.ludovicianul.timi.console;

import io.ludovicianul.timi.config.ConfigManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class Ansi {

  private final ConfigManager configManager;

  public static final String RESET = "\u001B[0m";
  public static final String BLACK = "\u001B[30m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String PURPLE = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";
  public static final String WHITE = "\u001B[37m";
  public static final String BOLD = "\u001B[1m";

  @Inject
  public Ansi(ConfigManager configManager) {
    this.configManager = configManager;
  }

  public String color256(String text, int code) {
    return configManager.isColorOutput() ? "\u001B[38;5;" + code + "m" + text + RESET : text;
  }

  public String green(String text) {
    return apply(text, GREEN);
  }

  public String red(String text) {
    return apply(text, RED);
  }

  public String purple(String text) {
    return apply(text, PURPLE);
  }

  public String yellow(String text) {
    return apply(text, YELLOW);
  }

  public String blue(String text) {
    return apply(text, BLUE);
  }

  public String cyan(String text) {
    return apply(text, CYAN);
  }

  public String bold(String text) {
    return apply(text, BOLD);
  }

  public String apply(String text, String... style) {
    if (!configManager.isColorOutput()) {
      return text;
    }
    return String.join("", style) + text + RESET;
  }
}
