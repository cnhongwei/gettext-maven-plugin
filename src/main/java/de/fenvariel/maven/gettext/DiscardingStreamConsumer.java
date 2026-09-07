package de.fenvariel.maven.gettext;

import org.codehaus.plexus.util.cli.StreamConsumer;

/** Consumes command output that is intentionally not needed by the plugin. */
final class DiscardingStreamConsumer implements StreamConsumer {

    @Override
    public void consumeLine(String line) {
        // Intentionally discard generated binary output.
    }
}
