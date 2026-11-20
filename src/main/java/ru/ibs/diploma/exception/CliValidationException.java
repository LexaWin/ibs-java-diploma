package ru.ibs.diploma.exception;

import java.util.Arrays;

/**
 * Проверяемое исключение для ошибок аргументов командной строки.
 * <p>Вместо готового пользовательского текста хранит ключ ресурса локализации
 * и строковые аргументы; отображение сообщения выполняется на уровне CLI.
 */
public class CliValidationException extends Exception {
    private final String messageKey; // Ключ локализации
    private final String[] arguments;

    /**
     * Создаёт ошибку валидации с ключом сообщения и параметрами форматирования.
     *
     * @param messageKey ключ сообщения в ресурсном bundle
     * @param arguments значения, подставляемые в шаблон локализации
     */
    public CliValidationException(String messageKey, Object... arguments) {
        super(messageKey);
        this.messageKey = messageKey;
        this.arguments = arguments == null ? new String[0]
                : Arrays.stream(arguments).map(String::valueOf).toArray(String[]::new);
    }

    /**
     * Возвращает ключ, по которому UI получает локализованный текст ошибки.
     *
     * @return ключ сообщения
     */
    public String messageKey() { return messageKey; }

    /**
     * Возвращает копию аргументов для форматирования сообщения.
     *
     * @return независимая копия массива строковых аргументов
     */
    public String[] arguments() { return arguments.clone(); }
}
