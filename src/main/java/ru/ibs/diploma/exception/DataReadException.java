package ru.ibs.diploma.exception;

import java.io.IOException;
import java.util.Arrays;

/**
 * Исключение, возникающее при чтении и разборе входных данных.
 * Хранит ключ сообщения локализации и аргументы для его форматирования.
 */
public class DataReadException extends IOException {
    private final String messageKey;
    private final String[] arguments;

    public DataReadException(String messageKey, Object... arguments) {
        this(messageKey, null, arguments);
    }

    public DataReadException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.arguments = arguments == null ? new String[0]
                : Arrays.stream(arguments).map(String::valueOf).toArray(String[]::new);
    }

    /**
     * Возвращает ключ сообщения локализации.
     */
    public String messageKey() {
        return messageKey;
    }

    /**
     * Возвращает копию аргументов сообщения.
     */
    public String[] arguments() {
        return arguments.clone();
    }
}