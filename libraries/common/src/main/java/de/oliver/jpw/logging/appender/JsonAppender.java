package de.oliver.jpw.logging.appender;

import de.oliver.jpw.logging.LogEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Local file-only structured logging. This class never performs network I/O. */
public final class JsonAppender implements Appender {
    private final Path path;

    public JsonAppender(boolean ignoredPretty, boolean ignoredColor, boolean ignoredAppend, String path) {
        this.path = Path.of(path);
    }

    @Override
    public void append(LogEntry entry) {
        String line = "{\"timestamp\":" + entry.timestamp()
                + ",\"logger\":\"" + escape(entry.loggerName())
                + "\",\"level\":\"" + entry.logLevel().name()
                + "\",\"message\":\"" + escape(entry.message()) + "\"}\n";
        try {
            Files.writeString(path, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[JPWLogger] Failed to append local log: " + e.getMessage());
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
