/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */

package microbat.codeanalysis.runtime.jpda.tty;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Internationalization (i18n) convenience methods for jdb.
 *
 * <p>All program output should flow through these methods, and this is the only class that should
 * be printing directly or otherwise accessing System.[out,err].
 *
 * @bug 4348376
 * @author Tim Bell
 */
public class MessageOutput {
  /**
   * The resource bundle containing localizable message content. This is loaded by TTY.main() at
   * start-up
   */
  static ResourceBundle textResources;

  /** Our message formatter. Allocated once, used many times */
  private static MessageFormat messageFormat;

  /** Fatal shutdown notification. This is sent to System.err instead of System.out */
  static void fatalError(String messageKey) {
    System.err.println();
    System.err.println(format("Fatal error"));
    System.err.println(format(messageKey));
    Env.shutdown();
  }

  /** "Format" a string by doing a simple key lookup. */
  static String format(String key) {
    return (textResources.getString(key));
  }

  /** Fetch and format a message with one string argument. This is the most common usage. */
  static String format(String key, String argument) {
    return format(key, new Object[] {argument});
  }

  /** Fetch a string by key lookup and format in the arguments. */
  static synchronized String format(String key, Object[] arguments) {
    if (messageFormat == null) {
      messageFormat = new MessageFormat(textResources.getString(key));
    } else {
      messageFormat.applyPattern(textResources.getString(key));
    }
    return (messageFormat.format(arguments));
  }

  /**
   * Print directly to System.out. Every rule has a few exceptions. The exceptions to "must use the
   * MessageOutput formatters" are: VMConnection.dumpStream() TTY.monitorCommand() TTY.TTY() (for
   * the '!!' command only) Commands.java (multiple locations) These are the only sites that should
   * be calling this method.
   */
  static void printDirectln(String line) {
    System.out.println(line);
  }

  static void printDirect(String line) {
    System.out.print(line);
  }

  static void printDirect(char c) {
    System.out.print(c);
  }

  /** Print a newline. Use this instead of '\n' */
  static void println() {
    System.out.println();
  }

  /** Format and print a simple string. */
  static void print(String key) {
    System.out.print(format(key));
  }

  /** Format and print a simple string. */
  static void println(String key) {
    System.out.println(format(key));
  }

  /** Fetch, format and print a message with one string argument. This is the most common usage. */
  static void print(String key, String argument) {
    System.out.print(format(key, argument));
  }

  static void println(String key, String argument) {
    System.out.println(format(key, argument));
  }

  /** Fetch, format and print a message with an arbitrary number of message arguments. */
  static void println(String key, Object[] arguments) {
    System.out.println(format(key, arguments));
  }

  /** Print a newline, followed by the string. */
  static void lnprint(String key) {
    System.out.println();
    System.out.print(textResources.getString(key));
  }

  static void lnprint(String key, String argument) {
    System.out.println();
    System.out.print(format(key, argument));
  }

  static void lnprint(String key, Object[] arguments) {
    System.out.println();
    System.out.print(format(key, arguments));
  }

  /** Print an exception message with a stack trace. */
  static void printException(String key, Exception e) {
    if (key != null) {
      try {
        println(key);
      } catch (MissingResourceException mex) {
        printDirectln(key);
      }
    }
    System.out.flush();
    e.printStackTrace();
  }

  static void printPrompt() {
    ThreadInfo threadInfo = ThreadInfo.getCurrentThreadInfo();
    if (threadInfo == null) {
      System.out.print(MessageOutput.format("jdb prompt with no current thread"));
    } else {
      System.out.print(
          MessageOutput.format(
              "jdb prompt thread name and current stack frame",
              new Object[] {
                threadInfo.getThread().name(), new Integer(threadInfo.getCurrentFrameIndex() + 1)
              }));
    }
    System.out.flush();
  }
}
