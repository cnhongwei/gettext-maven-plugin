package de.fenvariel.maven.gettext;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

/** Collects and renders gettext translation statistics for the console. */
final class TranslationStats {

    private static final Pattern TRANSLATED = Pattern.compile("(\\d+) translated message[s]?");
    private static final Pattern FUZZY = Pattern.compile("(\\d+) fuzzy translation[s]?");
    private static final Pattern UNTRANSLATED = Pattern.compile("(\\d+) untranslated message[s]?");

    private TranslationStats() {
    }

    static List<Entry> gather(File poDirectory, String keysFile, String msgfmtCmd,
                              String sourceLocale, Log log) {
        List<Entry> entries = new ArrayList<Entry>();
        if (poDirectory == null) {
            log.warn("Could not collect translation statistics: poDirectory is not configured.");
            return entries;
        }
        if (keysFile == null || keysFile.trim().isEmpty()) {
            keysFile = "keys.pot";
        }
        if (sourceLocale == null || sourceLocale.trim().isEmpty()) {
            sourceLocale = "en";
        }
        String normalizedSourceLocale = normalizeLocale(sourceLocale);
        File sourceFile = new File(poDirectory, keysFile);
        if (sourceFile.isFile()) {
            Entry source = read(sourceFile, normalizedSourceLocale, true, msgfmtCmd, log);
            if (source != null) {
                entries.add(source);
            }
        } else {
            log.warn("Could not find source catalog: " + sourceFile.getAbsolutePath());
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(poDirectory);
        scanner.setIncludes(new String[] {"**/*.po"});
        scanner.scan();
        for (String path : scanner.getIncludedFiles()) {
            String language = locale(path);
            if (normalizedSourceLocale.equals(language)) {
                continue;
            }
            Entry entry = read(new File(poDirectory, path), language, false, msgfmtCmd, log);
            if (entry != null) {
                entries.add(entry);
            }
        }
        Collections.sort(entries, new Comparator<Entry>() {
            public int compare(Entry left, Entry right) {
                if (left.source != right.source) {
                    return left.source ? 1 : -1;
                }
                return left.language.compareTo(right.language);
            }
        });
        return entries;
    }

    private static Entry read(File file, String language, boolean source, String msgfmtCmd, Log log) {
        Commandline command = new Commandline();
        command.addEnvironment("LC_ALL", "C");
        command.setExecutable(msgfmtCmd);
        command.createArg().setValue("--statistics");
        command.createArg().setValue(file.getAbsolutePath());

        Writer out = new StringWriter();
        Writer err = new StringWriter();
        try {
            int result = CommandLineUtils.executeCommandLine(command,
                    new WriterStreamConsumer(out), new WriterStreamConsumer(err));
            if (result != 0) {
                log.warn("Could not collect statistics for " + file.getAbsolutePath() + ": " + err);
                return null;
            }
            return parse(language, source, err.toString(), log);
        } catch (CommandLineException e) {
            log.warn("Could not execute " + msgfmtCmd + " for " + file.getAbsolutePath(), e);
            return null;
        }
    }

    private static Entry parse(String language, boolean source, String output, Log log) {
        String line = output.trim();
        int translated = number(TRANSLATED, line);
        int fuzzy = number(FUZZY, line);
        int untranslated = number(UNTRANSLATED, line);
        if (!TRANSLATED.matcher(line).find()
                && !FUZZY.matcher(line).find()
                && !UNTRANSLATED.matcher(line).find()) {
            log.warn("Could not parse statistic output: " + output);
            return null;
        }
        return new Entry(language, source, translated, fuzzy, untranslated);
    }

    private static int number(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static String locale(String path) {
        String name = new File(path).getName();
        return normalizeLocale(name.substring(0, name.lastIndexOf('.')));
    }

    private static String normalizeLocale(String value) {
        Locale locale = Locale.forLanguageTag(value.replace('_', '-'));
        String languageTag = locale.toLanguageTag();
        return languageTag.isEmpty() ? value.replace('_', '-') : languageTag;
    }

    static void print(List<Entry> entries, Log log) {
        if (entries.isEmpty()) {
            return;
        }
        String languageHeader = "Language";
        String totalHeader = "Total count";
        String missingHeader = "Missing";
        int languageWidth = languageHeader.length();
        int totalWidth = totalHeader.length();
        int missingWidth = missingHeader.length();
        for (Entry entry : entries) {
            languageWidth = Math.max(languageWidth, entry.displayLanguage().length());
            totalWidth = Math.max(totalWidth, String.valueOf(entry.total()).length());
            missingWidth = Math.max(missingWidth, entry.source ? 1 : String.valueOf(entry.missing()).length());
        }
        String top = "┌" + repeat('─', languageWidth + 2) + "┬"
                + repeat('─', totalWidth + 2) + "┬" + repeat('─', missingWidth + 2) + "┐";
        String middle = "├" + repeat('─', languageWidth + 2) + "┼"
                + repeat('─', totalWidth + 2) + "┼" + repeat('─', missingWidth + 2) + "┤";
        String bottom = "└" + repeat('─', languageWidth + 2) + "┴"
                + repeat('─', totalWidth + 2) + "┴" + repeat('─', missingWidth + 2) + "┘";
        log.info(top);
        log.info("│ " + padRight(languageHeader, languageWidth) + " │ "
                + padRight(totalHeader, totalWidth) + " │ " + padRight(missingHeader, missingWidth) + " │");
        log.info(middle);
        for (Entry entry : entries) {
            String missing = entry.source ? "-" : String.valueOf(entry.missing());
            log.info("│ " + padRight(entry.displayLanguage(), languageWidth) + " │ "
                    + padLeft(String.valueOf(entry.total()), totalWidth) + " │ " + padLeft(missing, missingWidth) + " │");
        }
        log.info(bottom);
    }

    private static String padRight(String value, int width) {
        StringBuilder result = new StringBuilder(value);
        while (result.length() < width) {
            result.append(' ');
        }
        return result.toString();
    }

    private static String padLeft(String value, int width) {
        StringBuilder result = new StringBuilder();
        while (result.length() + value.length() < width) {
            result.append(' ');
        }
        result.append(value);
        return result.toString();
    }

    private static String repeat(char character, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(character);
        }
        return result.toString();
    }

    static final class Entry {
        private final String language;
        private final boolean source;
        private final int translated;
        private final int fuzzy;
        private final int untranslated;

        Entry(String language, boolean source, int translated, int fuzzy, int untranslated) {
            this.language = language;
            this.source = source;
            this.translated = translated;
            this.fuzzy = fuzzy;
            this.untranslated = untranslated;
        }

        private String displayLanguage() {
            return source ? language + " (source)" : language;
        }

        private int total() {
            return translated + fuzzy + untranslated;
        }

        private int missing() {
            return fuzzy + untranslated;
        }
    }
}
