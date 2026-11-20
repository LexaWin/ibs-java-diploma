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
 * Загружает по запросу выбранные источники данных и преобразует их в общий
 * контейнер {@link LoadedData}.
 * <p>Для небольших наборов используется последовательное чтение, а при запросе
 * нескольких файлов суммарным размером от 5 MiB — параллельное. Сервис не
 * выполняет аналитические вычисления.
 */
@Service
public class DataLoadingService {
    /**
     * Минимальный суммарный размер запрошенных файлов, при котором чтение нескольких источников
     * переключается на параллельный режим. Значение равно 5 MiB.
     */
    private static final long PARALLEL_THRESHOLD_BYTES = 5L * 1024 * 1024;

    private final ParkingCsvReader csvReader;
    private final ParkingJsonReader jsonReader;
    private final PropertyCsvReader propertiesReader;
    private final PopulationTxtReader populationReader;
    private final ApplicationLogger logger;

    /**
     * Создаёт загрузчик, делегирующий разбор каждого источника соответствующему reader-компоненту.
     *
     * @param csvReader reader парковочных CSV-файлов
     * @param jsonReader reader парковочных JSON-файлов
     * @param propertiesReader reader CSV-файла недвижимости
     * @param populationReader reader текстового файла населения
     * @param logger журнал приложения
     */
    public DataLoadingService(ParkingCsvReader csvReader, ParkingJsonReader jsonReader,
                              PropertyCsvReader propertiesReader,
                              PopulationTxtReader populationReader, ApplicationLogger logger) {
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.propertiesReader = propertiesReader;
        this.populationReader = populationReader;
        this.logger = logger;
    }

    /** @return пустой контейнер без записей во всех трёх источниках */
    private static LoadedData empty() {
        return new LoadedData(List.of(), List.of(), List.of());
    }

    /**
     * Выбирает путь к файлу, соответствующему источнику данных.
     *
     * @param config конфигурация запуска
     * @param source источник данных
     * @return путь к файлу выбранного источника
     */
    private static Path path(CliConfig config, DataSource source) {
        return switch (source) {
            case PARKING -> config.getParkingFile();
            case PROPERTIES -> config.getPropertiesFile();
            case POPULATION -> config.getPopulationFile();
        };
    }

    /**
     * Адаптирует IO-операцию к API {@link CompletableFuture}, оборачивая checked-исключение.
     *
     * @param supplier операция чтения
     * @param <T> тип результата операции
     * @return результат операции
     * @throws UncheckedIOException если чтение завершилось ошибкой
     */
    private static <T> T readUnchecked(IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Открывает сессию ленивой загрузки без немедленного разбора входных файлов.
     *
     * @param config проверенная конфигурация путей и формата входных данных
     * @return новая сессия, кеширующая успешно загруженные источники
     * @throws IllegalArgumentException если конфигурация не задана
     */
    public DataLoadingSession openSession(CliConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Не задана конфигурация запуска");
        }
        return new DataLoadingSession(config, this);
    }

    /**
     * Загружает только указанные источники данных.
     * При нескольких источниках суммарным размером не менее 5 MiB выбирается параллельное чтение.
     *
     * @param config конфигурация путей и формата данных
     * @param sources непустой или пустой набор требуемых источников
     * @return контейнер с запрошенными данными; незапрошенные списки остаются пустыми
     * @throws IOException если размер файла невозможно получить или чтение источника завершилось ошибкой
     * @throws IllegalArgumentException если набор источников не задан
     */
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

    /**
     * Читает запрошенные источники отдельными задачами и объединяет результаты после их завершения.
     * Executor останавливается в блоке {@code finally}, в том числе при ошибке одной из задач.
     *
     * @param config конфигурация входных файлов
     * @param requestedSources источники, которые необходимо прочитать
     * @return контейнер с объединёнными результатами чтения
     * @throws IOException если одна из параллельных операций завершилась ошибкой
     */
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

    /**
     * Выбирает CSV- или JSON-reader в зависимости от формата в конфигурации.
     *
     * @param config конфигурация с путём и форматом парковочных данных
     * @return разобранные записи парковочных нарушений
     * @throws IOException если файл не удалось прочитать или разобрать
     */
    private List<ParkingViolation> readParking(CliConfig config) throws IOException {
        logger.log("DATA_FILE_OPEN source=parking path=" + config.getParkingFile());

        return "csv".equals(config.getParkingFormat())
                ? csvReader.read(config.getParkingFile())
                : jsonReader.read(config.getParkingFile());
    }

    /**
     * Читает исходный файл недвижимости.
     *
     * @param config конфигурация с путём к CSV-файлу недвижимости
     * @return разобранные записи об объектах недвижимости
     * @throws IOException если файл не удалось прочитать или его заголовки некорректны
     */
    private List<PropertyEntry> readProperties(CliConfig config) throws IOException {
        logger.log("DATA_FILE_OPEN source=properties path=" + config.getPropertiesFile());
        return propertiesReader.read(config.getPropertiesFile());
    }

    /**
     * Читает исходный файл численности населения.
     *
     * @param config конфигурация с путём к TXT-файлу населения
     * @return разобранные записи населения без предварительной группировки ZIP-кодов
     * @throws IOException если файл не удалось прочитать
     */
    private List<PopulationEntry> readPopulation(CliConfig config) throws IOException {
        logger.log("DATA_FILE_OPEN source=population path=" + config.getPopulationFile());
        return populationReader.read(config.getPopulationFile());
    }

    /**
     * Последовательно читает запрошенные источники и оставляет незапрошенные списки пустыми.
     *
     * @param config конфигурация входных файлов
     * @param requestedSources набор источников для чтения
     * @return контейнер с результатами загрузки
     * @throws IOException если чтение любого запрошенного источника завершилось ошибкой
     */
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

    /**
     * Функциональный контракт операции чтения, которая может завершиться {@link IOException}.
     * Используется для передачи файловых операций в асинхронные задачи.
     *
     * @param <T> тип результата операции
     */
    @FunctionalInterface
    private interface IoSupplier<T> {
        /**
         * Выполняет операцию чтения и может сообщить об ошибке ввода-вывода.
         *
         * @return результат чтения
         * @throws IOException если входной файл не удалось прочитать
         */
        T get() throws IOException;
    }
}
