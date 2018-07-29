package de.fenvariel.maven.gettext;

/*
 * Copyright 2005 by Steffen Pingel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

/**
 * Goal that generates a report.
 *
 * @author Steffen Pingel
 *
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ReportMojo extends AbstractMavenReport {

    /**
     * PO directory.
     */
    @Parameter(defaultValue = "src/main/po", required = true)
    protected File poDirectory;

    /**
     * msgfmt command.
     */
    @Parameter(defaultValue = "msgfmt", required = true)
    protected String msgfmtCmd;

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        Sink sink = getSink();

        sink.head();
        sink.title();
        sink.text("Gettext Statistics Report");
        sink.title_();
        sink.head_();

        sink.body();

        Stats stats = gatherStats();
        createReport(sink, stats);

        sink.body_();

        sink.flush();
        sink.close();
    }

    private void createReport(Sink sink, Stats stats) {
        sink.section1();
        sink.sectionTitle1();
        sink.text("Gettext Statistics");
        sink.sectionTitle1_();
        sink.section1_();

        sink.table();
        sink.tableCaption();
        sink.tableRow();
        sink.tableHeaderCell();
        sink.text("Locale");
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text("Translated");
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text("Untranslated");
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text("Fuzzy");
        sink.tableHeaderCell_();
        sink.tableRow_();

        List<StatsEntry> items = stats.getItems();
        Collections.sort(items);
        for (StatsEntry item : items) {
            sink.tableRow();
            // name
            sink.tableCell();
            sink.text(item.getLocale().getDisplayName());
            sink.tableCell_();
            // translated
            sink.tableCell();
            sink.text(item.getTranslated() + "");
            sink.tableCell_();
            // untranslated
            sink.tableCell();
            sink.text(item.getUntranslated() + "");
            sink.tableCell_();
            // fuzzy
            sink.tableCell();
            sink.text(item.getFuzzy() + "");
            sink.tableCell_();
            sink.tableRow_();
        }
        sink.table_();
    }

    @Override
    public String getDescription(Locale locale) {
        return "Statistics about po files.";
    }

    public String getName(Locale locale) {
        return "Gettext";
    }

    @Override
    public String getOutputName() {
        return "gettext-report";
    }

    public Stats gatherStats() {
        getLog().info("Gathering statistics for po files in '"
                + poDirectory.getAbsolutePath() + "'.");

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(poDirectory);
        ds.setIncludes(new String[]{"**/*.po"});
        ds.scan();

        Stats stats = new Stats();

        String[] files = ds.getIncludedFiles();
        for (String file1 : files) {
            File file = new File(poDirectory, file1);
            getLog().info("Processing " + file.getAbsolutePath());

            Commandline cl = new Commandline();
            // make sure the output is in english
            cl.addEnvironment("LC_ALL", "C");
            cl.setExecutable(msgfmtCmd);
            cl.createArg().setValue("--statistics");
            cl.createArg().setValue(file.getAbsolutePath());

            Writer out = new StringWriter();
            Writer err = new StringWriter();
            try {
                int ret = CommandLineUtils.executeCommandLine(cl,
                        new WriterStreamConsumer(out),
                        new WriterStreamConsumer(err));
                if (ret == 0) {
                    // for whatever reason the output is written to stderr
                    stats.parseOutput(file, err.toString());
                } else {
                    getLog().info(err.toString());
                }
            } catch (CommandLineException e) {
                getLog().error("Could not execute msgfmt: " + err.toString(), e);
            }
        }

        return stats;
    }

    public static Locale getLocale(File file) {
        String basename = file.getName().substring(0, file.getName().lastIndexOf('.'));
        return Locale.forLanguageTag(basename);
    }

    private static Pattern patternTranslated = Pattern.compile("(\\d+) translated message[s]?");
    private static Pattern patternFuzzy =  Pattern.compile("(\\d+) fuzzy translation[s]?");
    private static Pattern patternUntranslated = Pattern.compile("(\\d+) untranslated message[s]?");
        
    public class Stats {

        private final List<StatsEntry> items;

        private Stats() {
            this.items = new ArrayList<StatsEntry>();
        }

        /**
         * <code>
         *  117 translated messages.
         *  0 translated messages, 117 untranslated messages.
         * 	92 translated messages, 5 fuzzy translations, 20 untranslated messages.
         * </code>
         *
         * @param line output of msgfmt command
         */
        private void parseOutput(File file, String line) {
            StatsEntry entry = new StatsEntry(file);
            items.add(entry);

            
            Matcher matcher = patternTranslated.matcher(line.trim());
            if (!matcher.find()) {
                getLog().error("Could not parse statistic output: " + line);
            } else {
                entry.setTranslated(extractNumber(matcher.group(1)));
            }
            matcher = patternFuzzy.matcher(line.trim());
            if (matcher.find()) {
                entry.setFuzzy(extractNumber(matcher.group(1)));
            }
            matcher = patternUntranslated.matcher(line.trim());
            if (matcher.find()) {
                entry.setUntranslated(extractNumber(matcher.group(1)));
            }
        }

        private int extractNumber(String token) {
            StringTokenizer t = new StringTokenizer(token, " ");
            if (t.hasMoreTokens()) {
                try {
                    return Integer.parseInt(t.nextToken());
                } catch (NumberFormatException e) {
                    getLog().error("Could not parse token \"" + token + "\":" + e.getMessage());
                }
            }
            getLog().warn("Could not extract number from: " + token);
            return 0;
        }

        public List<StatsEntry> getItems() {
            return items;
        }

    }

    public class StatsEntry implements Comparable<StatsEntry> {

        private final File file;
        private final Locale locale;
        private int untranslated;
        private int fuzzy;
        private int translated;

        public StatsEntry(File file) {
            this.file = file;
            this.locale = ReportMojo.getLocale(file);
        }

        public int compareTo(StatsEntry o) {
            return getLocale().getDisplayName().compareTo(
                    o.getLocale().getDisplayName());
        }

        public Locale getLocale() {
            return locale;
        }

        public File getFile() {
            return file;
        }

        public int getTotal() {
            return getUntranslated() + getFuzzy() + getTranslated();
        }

        public int getUntranslated() {
            return untranslated;
        }

        public int getFuzzy() {
            return fuzzy;
        }

        public int getTranslated() {
            return translated;
        }

        private void setTranslated(int translated) {
            this.translated = translated;
        }

        private void setFuzzy(int fuzzy) {
            this.fuzzy = fuzzy;
        }

        private void setUntranslated(int untranslated) {
            this.untranslated = untranslated;
        }

    }
}
