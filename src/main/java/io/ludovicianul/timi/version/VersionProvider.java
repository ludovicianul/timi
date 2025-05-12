package io.ludovicianul.timi.version;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {
  public static final String VERSION = "1.0.5";
  public static final String DATE = "2025-05-12";

  @Override
  public String[] getVersion() {
    return new String[] {"timi CLI v" + VERSION};
  }
}
