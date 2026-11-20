package ru.ibs.diploma.processor;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Потокобезопасный кеш результатов вычислений в пределах одного экземпляра приложения.
 * <p>Для каждого ключа {@link ConcurrentHashMap#computeIfAbsent(Object, java.util.function.Function)}
 * выполняет поставщик при отсутствии значения. Один ключ должен всегда обозначать
 * значение одного и того же типа: тип результата хранится как {@link Object} и
 * приводится к параметру типа {@code T} без проверки во время выполнения.
 */
@Component
public class ComputationCache {
    private final ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();

    /**
     * Возвращает сохранённое значение или вычисляет и сохраняет его при отсутствии ключа.
     * Обобщённое приведение из {@link Object} не проверяет фактический тип во время выполнения,
     * поэтому вызывающий код обязан использовать один ключ только для одного типа результата.
     *
     * @param key ключ кеша, уникальный в рамках семейства и типа результата
     * @param calculation вычисление, выполняемое при отсутствии значения
     * @param <T> ожидаемый тип результата
     * @return кешированное или только что вычисленное значение
     */
    public <T> T get(String key, Supplier<T> calculation) {
        return (T) values.computeIfAbsent(key, k -> calculation.get());
    }

    /**
     * Возвращает количество различных ключей, сохранённых в кеше.
     *
     * @return число кешированных значений
     */
    public int size() {
        return values.size();
    }
}
