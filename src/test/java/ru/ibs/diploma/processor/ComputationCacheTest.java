package ru.ibs.diploma.processor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComputationCacheTest {
    @Test
    void concurrentRequestsForOneKeyRunSupplierOnce() throws Exception {
        ComputationCache cache = new ComputationCache();
        AtomicInteger calls = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            var tasks = java.util.stream.IntStream.range(0, 32)
                    .<Callable<Integer>>mapToObj(i -> () -> cache.get("same", () -> {
                        calls.incrementAndGet();
                        return 42;
                    })).toList();
            for (Future<Integer> future : pool.invokeAll(tasks)) assertEquals(42, future.get().intValue());
            assertEquals(1, calls.get());
        } finally {
            pool.shutdownNow();
        }
    }
}
