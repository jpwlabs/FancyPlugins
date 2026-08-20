package de.oliver.jpw.logging.properties;

public final class ThrowableProperty extends Property<Throwable> {
    public ThrowableProperty(Throwable value) {
        super("throwable", value);
    }

    public static ThrowableProperty of(Throwable value) {
        return new ThrowableProperty(value);
    }
}
