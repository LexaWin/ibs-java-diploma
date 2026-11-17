package ru.ibs.diploma.datamanagement;

import org.springframework.stereotype.Service;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.data.PropertyEntry;
import ru.ibs.diploma.logging.ApplicationLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.*;

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
    private final ApplicationLogger logger;

    public DataLoadingService(ParkingCsvReader csvReader, ParkingJsonReader jsonReader,
                              PropertyCsvReader propertiesReader,
                              PopulationTxtReader populationReader, ApplicationLogger logger) {
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.propertiesReader = propertiesReader;
        this.populationReader = populationReader;
        this.logger = logger;
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

    private static <T> T readUnchecked(IoSupplier<T> supplier) {
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
        if (sources.size() > 1 && bytes >= PARALLEL_THRESHOLD_BYTES) {
            logger.log("DATA_LOADING mode=parallel bytes=" + bytes + " sources=" + sources);
            return loadInParallel(config, sources);
        }

        logger.log("DATA_LOADING mode=sequential bytes=" + bytes + " sources=" + sources);

        return loadSequentially(config, sources);
    }

    private LoadedData loadInParallel(CliConfig config, EnumSet<DataSource> requestedSources)
            throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(requestedSources.size(), task -> {
            Thread thread = new Thread(task, "data-loader");
            thread.setDaemon(false);
            return thread;
        });

        CompletableFuture<List<ParkingViolation>> parking = requestedSources.contains(DataSource.PARKING)
                ? CompletableFuture.supplyAsync(() -> readUnchecked(() -> readParking(config)), executor)
                : CompletableFuture.completedFuture(List.of());
        CompletableFuture<List<PropertyEntry>> properties = requestedSources.contains(DataSource.PROPERTIES)
                ? CompletableFuture.supplyAsync(() -> readUnchecked(() -> readProperties(config)), executor)
                : CompletableFuture.completedFuture(List.of());
        CompletableFuture<List<PopulationEntry>> population = requestedSources.contains(DataSource.POPULATION)
                ? CompletableFuture.supplyAsync(() -> readUnchecked(() -> readPopulation(config)), executor)
                : CompletableFuture.completedFuture(List.of());

        try {
            CompletableFuture.allOf(parking, properties, population).join();
            return new LoadedData(parking.join(), properties.join(), population.join());
        } catch (CompletionException ex) {
            parking.cancel(true);
            properties.cancel(true);
            population.cancel(true);
            Throwable cause = ex.getCause();
            if (cause instanceof UncheckedIOException io) throw io.getCause();
            throw new IOException("Не удалось загрузить выбранные источники данных", cause);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) executor.shutdownNow();
            } catch (InterruptedException ex) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<ParkingViolation> readParking(CliConfig config) throws IOException {
        logger.log("DATA_FILE_OPEN source=parking path=" + config.getParkingFile());

        return "csv".equals(config.getParkingFormat())
                ? csvReader.read(config.getParkingFile())
                : jsonReader.read(config.getParkingFile());
    }

    private List<PropertyEntry> readProperties(CliConfig config) throws IOException {
        logger.log("DATA_FILE_OPEN source=properties path=" + config.getPropertiesFile());
        return propertiesReader.read(config.getPropertiesFile());
    }

    private List<PopulationEntry> readPopulation(CliConfig config) throws IOException {
        logger.log("DATA_FILE_OPEN source=population path=" + config.getPopulationFile());
        return populationReader.read(config.getPopulationFile());
    }

    private LoadedData loadSequentially(CliConfig config, EnumSet<DataSource> requestedSources)
            throws IOException {
        List<ParkingViolation> parking = requestedSources.contains(DataSource.PARKING)
                ? readParking(config) : List.of();
        List<PropertyEntry> properties = requestedSources.contains(DataSource.PROPERTIES)
                ? readProperties(config) : List.of();
        List<PopulationEntry> population = requestedSources.contains(DataSource.POPULATION)
                ? readPopulation(config) : List.of();
        return new LoadedData(parking, properties, population);
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
