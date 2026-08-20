package de.oliver.jpw.logging.middleware;

import de.oliver.jpw.logging.LogEntry;

@FunctionalInterface
public interface Middleware {
    LogEntry process(LogEntry logEntry);
}
