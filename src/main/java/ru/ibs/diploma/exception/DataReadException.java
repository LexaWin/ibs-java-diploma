package ru.ibs.diploma.exception;

import java.io.IOException;
import java.util.Arrays;

/**
 * Ошибка чтения или структурного разбора входных данных.
 * <p>Наследуется от {@link IOException}, сохраняя совместимость с файловыми
 * операциями, и дополнительно содержит ключ локализации и аргументы сообщения.
 */
public class DataReadException extends IOException {
    private final String messageKey;
    private final String[] arguments;

    /**
     * Создаёт ошибку чтения без отдельной первопричины.
     *
     * @param messageKey ключ сообщения локализации
     * @param arguments аргументы шаблона сообщения
     */
    public DataReadException(String messageKey, Object... arguments) {
        this(messageKey, null, arguments);
    }

    /**
     * Создаёт ошибку чтения с сохранением исходного исключения.
     *
     * @param messageKey ключ сообщения локализации
     * @param cause исходная ошибка разбора или чтения
     * @param arguments аргументы шаблона сообщения
     */
    public DataReadException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.arguments = arguments == null ? new String[0]
                : Arrays.stream(arguments).map(String::valueOf).toArray(String[]::new);
    }

    /**
     * Возвращает ключ сообщения для локализованного отображения на UI-уровне.
     *
     * @return ключ ресурсного сообщения
     */
    public String messageKey() {
        return messageKey;
    }

    /**
     * Возвращает копию аргументов, используемых для форматирования сообщения.
     *
     * @return копия массива строковых аргументов
     */
    public String[] arguments() {
        return arguments.clone();
    }
}