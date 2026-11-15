package ru.ibs.diploma.datamanagement;

import org.springframework.stereotype.Service;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.data.PropertyEntry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Читает только явно запрошенные источники и возвращает разобранные записи.
 */
@Service
public class DataLoadingService {
    private static final long PARALLEL_THRESHOLD_BYTES = 5L * 1024 * 1024;
    private final ParkingCsvReader csvReader;
    private final ParkingJsonReader jsonReader;
    private final PropertyCsvReader propertiesReader;
    private final PopulationTxtReader populationReader;

    public DataLoadingService(ParkingCsvReader csvReader, ParkingJsonReader jsonReader,
                              PropertyCsvReader propertiesReader, PopulationTxtReader populationReader) {
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.propertiesReader = propertiesReader;
        this.populationReader = populationReader;
    }

    private static LoadedData empty() {
        return new LoadedData(List.of(), List.of(), List.of());
    }

    private static Path path(CliConfig config, DataSource source) {
        return switch (source) {
            case PARKING -> config.getParkingFile();
            case PROPERTIES -> config.getPropertiesFile();
            case POPULATION -> config.getPopulationFile();
        };
    }

    private static <T> T unchecked(IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Создаёт пустую сессию; чтение файлов начнётся только при запросе источника.
     */
    public DataLoadingSession openSession(CliConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Не задана конфигурация запуска");
        }
        return new DataLoadingSession(config, this);
    }

    public LoadedData load(CliConfig config, EnumSet<DataSource> sources) throws IOException {
        if (sources == null)
            throw new IllegalArgumentException("Не задан набор источников");
        if (sources.isEmpty())
            return empty();
        long bytes = 0;
        for (DataSource source : sources) {
            bytes += Files.size(path(config, source));
        }
        if (sources.size() > 1 && bytes >= PARALLEL_THRESHOLD_BYTES)
            return parallel(config, sources);
        return sequential(config, sources);
    }

    private LoadedData parallel(CliConfig config, EnumSet<DataSource> sources) throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(sources.size());
        CompletableFuture<List<ParkingViolation>> p = sources.contains(DataSource.PARKING)
                ? CompletableFuture.supplyAsync(() -> unchecked(() -> readParking(config)), pool)
                : CompletableFuture.completedFuture(List.of());
        CompletableFuture<List<PropertyEntry>> pr = sources.contains(DataSource.PROPERTIES)
                ? CompletableFuture.supplyAsync(() -> unchecked(() -> propertiesReader.read(config.getPropertiesFile())), pool)
                : CompletableFuture.completedFuture(List.of());
        CompletableFuture<List<PopulationEntry>> po = sources.contains(DataSource.POPULATION)
                ? CompletableFuture.supplyAsync(() -> unchecked(() -> populationReader.read(config.getPopulationFile())), pool)
                : CompletableFuture.completedFuture(List.of());
        try {
            CompletableFuture.allOf(p, pr, po).join();
            return new LoadedData(p.join(), pr.join(), po.join());
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UncheckedIOException io)
                throw io.getCause();
            throw new IOException("Не удалось загрузить выбранные данные", cause);
        } finally {
            pool.shutdownNow();
        }
    }

    private List<ParkingViolation> readParking(CliConfig config) throws IOException {
        return "csv".equals(config.getParkingFormat())
                ? csvReader.read(config.getParkingFile())
                : jsonReader.read(config.getParkingFile());
    }

    private LoadedData sequential(CliConfig config, EnumSet<DataSource> sources) throws IOException {
        List<ParkingViolation> parkingViolations = sources.contains(DataSource.PARKING)
                ? readParking(config)
                : List.of();
        List<PropertyEntry> properties = sources.contains(DataSource.PROPERTIES)
                ? propertiesReader.read(config.getPropertiesFile())
                : List.of();
        List<PopulationEntry> populations = sources.contains(DataSource.POPULATION)
                ? populationReader.read(config.getPopulationFile())
                : List.of();
        return new LoadedData(parkingViolations, properties, populations);
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
