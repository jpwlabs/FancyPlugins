package de.oliver.jpw.logging.appender;

import de.oliver.jpw.logging.LogEntry;

public final class ConsoleAppender implements Appender {
    private final String template;

    public ConsoleAppender() {
        this("[{loggerName}] {logLevel}: {message}");
    }

    public ConsoleAppender(String template) {
        this.template = template;
    }

    @Override
    public void append(LogEntry entry) {
        String output = template
                .replace("{loggerName}", entry.loggerName())
                .replace("{threadName}", Thread.currentThread().getName())
                .replace("{logLevel}", entry.logLevel().name())
                .replace("{message}", entry.message());
        System.out.println(output);
        Object thrown = entry.properties().get("throwable");
        if (thrown instanceof Throwable throwable) {
            throwable.printStackTrace(System.out);
        }
    }
}
