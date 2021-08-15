package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

  public static final String USAGE = "Usage: jlox [script]";
  public static final int USAGE_ERROR_CODE = 64;
  public static final int DATA_ERROR_CODE = 65;
  public static final String PROMPT = "> ";
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static boolean hadError;

  public static void main(final String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println(Lox.USAGE);
      System.exit(USAGE_ERROR_CODE);
    } else if (args.length == 1) {
      runFile(Paths.get(args[0]));
    } else {
      runPrompt();
    }
  }

  private static void runPrompt() throws IOException {
    final InputStreamReader input = new InputStreamReader(System.in);
    final BufferedReader reader = new BufferedReader(input);
    while (true) {
      System.out.print(PROMPT);
      final String line = reader.readLine();
      if (line == null) {
        break;
      }
      run(line);
    }
  }

  private static void runFile(final Path file) throws IOException {
    final byte[] bytes = Files.readAllBytes(file);
    run(new String(bytes, CHARSET));
    hadError = false;
  }

  private static void run(final String source) {
    final Scanner scanner = new Scanner(source);
    final List<Token> tokens = scanner.scanTokens();

    if (hadError) {
      System.exit(DATA_ERROR_CODE);
    }
    for (final Token token : tokens) {
      System.out.println(token);
    }
  }

  static void error(final int line, final String message) {
    report(line, "", message);
  }

  private static void report(final int line, final String where, final String message) {
    System.err.println("[Line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }
}
