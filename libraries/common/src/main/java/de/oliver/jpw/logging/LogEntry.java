package de.oliver.jpw.logging;

import java.util.LinkedHashMap;
import java.util.Map;

public record LogEntry(
        String loggerName,
        LogLevel logLevel,
        String message,
        long timestamp,
        Map<String, Object> properties
) {
    public LogEntry {
        properties = properties == null ? new LinkedHashMap<>() : new LinkedHashMap<>(properties);
    }

    public LogEntry addProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }
}
