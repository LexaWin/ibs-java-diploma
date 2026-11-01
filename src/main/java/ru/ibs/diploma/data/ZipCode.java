package ru.ibs.diploma.data;

/**
 * Единая нормализация ZIP-кода до пяти начальных цифр.
 */
public final class ZipCode {
    private ZipCode() {
    }

    public static String normalize(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.length() < 5) return null;
        String prefix = value.substring(0, 5);
        return prefix.matches("\\d{5}") ? prefix : null;
    }
}
