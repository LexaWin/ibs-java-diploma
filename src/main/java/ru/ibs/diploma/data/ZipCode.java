package ru.ibs.diploma.data;

/**
 * Утилита нормализации ZIP-кодов для согласования записей из разных источников.
 * <p>Первые пять символов должны быть цифрами; дополнительные символы после
 * этого префикса не проверяются. Некорректное значение представляется {@code null}.
 */
public final class ZipCode {
    /** Запрещает создание экземпляров утилитного класса. */
    private ZipCode() {
    }

    /**
     * Извлекает первые пять символов и проверяет, что каждый из них является цифрой.
     *
     * @param raw исходное значение ZIP-кода
     * @return пятизначный префикс либо {@code null}, если значение отсутствует, короткое или нечисловое
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.length() < 5) return null;
        String prefix = value.substring(0, 5);
        return prefix.matches("\\d{5}") ? prefix : null;
    }
}
