package ru.ibs.diploma.processor;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Кеш результатов в рамках одного запуска.
 * Одинаковый ключ вычисляется один раз.
 */
@Component
public class ComputationCache {
    private final ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();

    public <T> T get(String key, Supplier<T> calculation) {
        return (T) values.computeIfAbsent(key, k -> calculation.get());
    }

    public int size() {
        return values.size();
    }
}
