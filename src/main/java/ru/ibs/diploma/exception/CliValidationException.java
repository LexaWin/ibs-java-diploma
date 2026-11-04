package ru.ibs.diploma.exception;

import java.util.Arrays;

/** Ошибка проверки параметров CLI. */
public class CliValidationException extends Exception {
    private final String messageKey; // Ключ локализации
    private final String[] arguments;

    public CliValidationException(String messageKey, Object... arguments) {
        super(messageKey);
        this.messageKey = messageKey;
        this.arguments = arguments == null ? new String[0]
                : Arrays.stream(arguments).map(String::valueOf).toArray(String[]::new);
    }

    /**
     * Возвращает ключ сообщения локализации.
     */
    public String messageKey() { return messageKey; }

    /**
     * Возвращает копию аргументов сообщения.
     */
    public String[] arguments() { return arguments.clone(); }
}
