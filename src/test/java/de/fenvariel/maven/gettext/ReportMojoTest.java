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
import java.util.Locale;

import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author AlexS
 */
public class ReportMojoTest {

    private ReportMojo reportMojo;

    public ReportMojoTest() {
        reportMojo = new ReportMojo();
        reportMojo.msgfmtCmd = "msgfmt";
        reportMojo.poDirectory = new File("src/test/resources");
    }

    /**
     * Test of gatherStats method, of class ReportMojo.
     */
    @Test
    public void testGatherStats() {
        System.out.println("gatherStats");
        ReportMojo.Stats result = reportMojo.gatherStats();
        Assert.assertEquals(4, result.getItems().size());
        for (ReportMojo.StatsEntry e : result.getItems()) {
            Assert.assertNotNull(e.getLocale());
            String languageTag = e.getLocale().toLanguageTag();
            if ("de".equals(languageTag)) {
                assert_de(e);
            }
            if ("de_AT".equals(languageTag)) {
                assert_de_AT(e);
            }
            if ("de_DE".equals(languageTag)) {
                assert_de_DE(e);
            }
            if ("fr".equals(languageTag)) {
                assert_fr(e);
            }
        }

    }

    @Test
    public void testGeneratedReportContainsStatisticsTable() throws Exception {
        SiteRendererSink sink = new SiteRendererSink(
                new RenderingContext(new File("target"), "gettext-report.html"));

        reportMojo.generate(sink, Locale.ENGLISH);

        String report = sink.getBody();
        Assert.assertTrue(report.contains("<table"));
        Assert.assertTrue(report.contains("<th>Locale</th>"));
        Assert.assertTrue(report.contains("<td>German</td>"));
    }

    @Test
    public void testGetLocaleAcceptsUnderscoreSeparatedFileNames() {
        Locale locale = ReportMojo.getLocale(new File("zh_CN.po"));

        Assert.assertEquals("zh-CN", locale.toLanguageTag());
    }

    private void assert_de(ReportMojo.StatsEntry e) {
        Assert.assertEquals(1, e.getTranslated());
        Assert.assertEquals(1, e.getFuzzy());
        Assert.assertEquals(1, e.getUntranslated());
        Assert.assertEquals(e.getTotal(), e.getTranslated() + e.getFuzzy() + e.getUntranslated());
    }
    
    private void assert_fr(ReportMojo.StatsEntry e) {
        Assert.assertEquals(0, e.getTranslated());
        Assert.assertEquals(0, e.getFuzzy());
        Assert.assertEquals(11, e.getUntranslated());
        Assert.assertEquals(e.getTotal(), e.getTranslated() + e.getFuzzy() + e.getUntranslated());
    }
    
    private void assert_de_DE(ReportMojo.StatsEntry e) {
        Assert.assertEquals(11, e.getTranslated());
        Assert.assertEquals(0, e.getFuzzy());
        Assert.assertEquals(0, e.getUntranslated());
        Assert.assertEquals(e.getTotal(), e.getTranslated() + e.getFuzzy() + e.getUntranslated());
    }
    
    private void assert_de_AT(ReportMojo.StatsEntry e) {
        Assert.assertEquals(0, e.getTranslated());
        Assert.assertEquals(11, e.getFuzzy());
        Assert.assertEquals(0, e.getUntranslated());
        Assert.assertEquals(e.getTotal(), e.getTranslated() + e.getFuzzy() + e.getUntranslated());
    }

}
