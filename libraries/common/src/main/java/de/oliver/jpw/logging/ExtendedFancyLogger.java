package de.oliver.jpw.logging;

import de.oliver.jpw.logging.appender.Appender;
import de.oliver.jpw.logging.appender.ConsoleAppender;
import de.oliver.jpw.logging.middleware.Middleware;
import de.oliver.jpw.logging.properties.Property;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal local logger replacing the upstream bundled telemetry logger dependency. */
public class ExtendedFancyLogger {
    private final String name;
    private final List<Appender> appenders;
    private final List<Middleware> middleware;
    private LogLevel currentLevel;

    public ExtendedFancyLogger(String name) {
        this(name, LogLevel.INFO, List.of(new ConsoleAppender()), List.of());
    }

    public ExtendedFancyLogger(String name, LogLevel currentLevel,
                               List<Appender> appenders, List<Middleware> middleware) {
        this.name = name;
        this.currentLevel = currentLevel;
        this.appenders = new ArrayList<>(appenders);
        this.middleware = new ArrayList<>(middleware);
    }

    public void log(LogLevel level, String message, Property<?>... properties) {
        if (level.ordinal() < currentLevel.ordinal()) {
            return;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (Property<?> property : properties) {
            values.put(property.getKey(), property.getValue());
        }
        LogEntry entry = new LogEntry(name, level, message, System.currentTimeMillis(), values);
        for (Middleware item : middleware) {
            entry = item.process(entry);
            if (entry == null) {
                return;
            }
        }
        for (Appender appender : appenders) {
            appender.append(entry);
        }
    }

    public void debug(String message, Property<?>... properties) { log(LogLevel.DEBUG, message, properties); }
    public void info(String message, Property<?>... properties) { log(LogLevel.INFO, message, properties); }
    public void warn(String message, Property<?>... properties) { log(LogLevel.WARN, message, properties); }
    public void error(String message, Property<?>... properties) { log(LogLevel.ERROR, message, properties); }
    public void setCurrentLevel(LogLevel level) { currentLevel = level; }
    public void addAppender(Appender appender) { appenders.add(appender); }
    public void addMiddlware(Middleware item) { middleware.add(item); }
    public String getName() { return name; }
}
