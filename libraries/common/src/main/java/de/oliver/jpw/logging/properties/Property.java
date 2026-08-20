package de.oliver.jpw.logging.properties;

public class Property<T> {
    private final String key;
    public final T value;

    public Property(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }
}
