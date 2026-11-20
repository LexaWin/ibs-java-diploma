package ru.ibs.diploma.ui;

import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Предоставляет пользовательские сообщения из {@link ResourceBundle}.
 * <p>Базовый набор ресурсов является английским, а дополнительные локали задаются
 * отдельными файлами, например {@code messages_ru.properties}. Для неподдерживаемой
 * локали используется базовый набор без скрытого перехода к системной локали JVM.
 */
@Component
public class LocalizationService {
    private final Locale locale;
    private final ResourceBundle bundle;

    /**
     * Создаёт локализатор, использующий локаль по умолчанию текущей JVM.
     */
    public LocalizationService() {
        this(Locale.getDefault());
    }

    /**
     * Создаёт локализатор для указанной локали без автоматического fallback к локали JVM.
     *
     * @param systemLocale локаль, определяющая выбор ресурсного bundle и форматирование
     */
    public LocalizationService(Locale systemLocale) {
        this.locale = systemLocale;
        // Не позволяет ResourceBundle незаметно использовать системную локаль JVM, если запрошенный язык недоступен.
        // Базовый файл messages.properties служит английским вариантом для неподдерживаемых локалей.
        ResourceBundle.Control control = ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_DEFAULT);
        this.bundle = ResourceBundle.getBundle("i18n.messages", systemLocale, control);
    }

    /**
     * Возвращает локализованный текст и подставляет аргументы в шаблон {@link MessageFormat}.
     * Если ключ отсутствует, в качестве текста используется сам ключ.
     *
     * @param key ключ сообщения в ресурсном bundle
     * @param args аргументы для подстановки в шаблон
     * @return локализованная и отформатированная строка
     */
    public String text(String key, Object... args) {
        String pattern = bundle.containsKey(key) ? bundle.getString(key) : key;
        return new MessageFormat(pattern, locale).format(args == null ? new Object[0] : args);
    }
}
