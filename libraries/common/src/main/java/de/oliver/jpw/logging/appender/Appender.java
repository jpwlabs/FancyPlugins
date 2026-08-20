package de.oliver.jpw.logging.appender;

import de.oliver.jpw.logging.LogEntry;

public interface Appender extends AutoCloseable {
    void append(LogEntry logEntry);

    @Override
    default void close() {
    }
}
