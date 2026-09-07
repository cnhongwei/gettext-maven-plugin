package de.fenvariel.maven.gettext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class TranslationStatsTest {

    @Test
    public void doesNotReportSourceCatalogAgainAsTranslation() throws Exception {
        Path directory = Files.createTempDirectory("gettext-stats");
        try {
            String catalog = "msgid \"\"\nmsgstr \"\"\n\n"
                    + "msgid \"Hello\"\nmsgstr \"\"\n";
            Files.write(directory.resolve("keys.pot"), catalog.getBytes(StandardCharsets.UTF_8));
            Files.write(directory.resolve("zh_CN.po"), catalog.getBytes(StandardCharsets.UTF_8));

            List<TranslationStats.Entry> entries = TranslationStats.gather(
                    directory.toFile(), "keys.pot", "msgfmt", "zh_CN", new SystemStreamLog());

            Assert.assertEquals(1, entries.size());
        } finally {
            FileUtils.deleteDirectory(directory.toFile());
        }
    }
}
