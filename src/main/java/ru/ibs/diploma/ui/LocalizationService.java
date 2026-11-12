package ru.ibs.diploma.ui;

import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Локализация на основе ResourceBundle.
 * Для добавления, например, немецкого языка достаточно создать messages_de.properties.
 */
@Component
public class LocalizationService {
    private final Locale locale;
    private final ResourceBundle bundle;

    public LocalizationService() {
        this(Locale.getDefault());
    }

    public LocalizationService(Locale systemLocale) {
        this.locale = systemLocale;
        // Не позволяет ResourceBundle незаметно использовать системную локаль JVM, если запрошенный язык недоступен.
        // Базовый файл messages.properties служит английским вариантом для неподдерживаемых локалей.
        ResourceBundle.Control control = ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_DEFAULT);
        this.bundle = ResourceBundle.getBundle("i18n.messages", systemLocale, control);
    }

    public String text(String key, Object... args) {
        String pattern = bundle.containsKey(key) ? bundle.getString(key) : key;
        return new MessageFormat(pattern, locale).format(args == null ? new Object[0] : args);
    }
}
