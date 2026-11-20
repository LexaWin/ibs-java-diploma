package ru.ibs.diploma.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import ru.ibs.diploma.exception.CliValidationException;

/**
 * Неизменяемая конфигурация одного запуска CLI-приложения.
 * <p>Содержит формат и пути к трём входным наборам данных, а также путь к журналу.
 * Экземпляр создаётся только после проверки количества аргументов, формата
 * парковочного файла и доступности всех входных файлов.
 */
public class CliConfig {
    private final String parkingFormat;
    private final Path parkingFile;
    private final Path propertiesFile;
    private final Path populationFile;
    private final Path logFile;

    /**
     * Создаёт конфигурацию после предварительной проверки всех аргументов.
     *
     * @param parkingFormat формат парковочного файла: {@code csv} или {@code json}
     * @param parkingFile путь к файлу парковочных нарушений
     * @param propertiesFile путь к CSV-файлу недвижимости
     * @param populationFile путь к текстовому файлу населения
     * @param logFile путь к файлу журнала
     */
    private CliConfig(String parkingFormat, Path parkingFile, Path propertiesFile,
                      Path populationFile, Path logFile) {
        this.parkingFormat = parkingFormat;
        this.parkingFile = parkingFile;
        this.propertiesFile = propertiesFile;
        this.populationFile = populationFile;
        this.logFile = logFile;
    }

    /** @return выбранный формат парковочных данных: {@code csv} или {@code json} */
    public String getParkingFormat() { return parkingFormat; }

    /** @return путь к файлу парковочных нарушений */
    public Path getParkingFile() { return parkingFile; }

    /** @return путь к CSV-файлу недвижимости */
    public Path getPropertiesFile() { return propertiesFile; }

    /** @return путь к текстовому файлу населения */
    public Path getPopulationFile() { return populationFile; }

    /** @return путь к файлу журнала приложения */
    public Path getLogFile() { return logFile; }

    /**
     * Разбирает и проверяет аргументы командной строки.
     * Ожидается ровно пять аргументов; формат должен быть строго {@code csv} или {@code json}.
     * Все три входных файла проверяются на существование, тип обычного файла и доступность для чтения.
     *
     * @param args исходные аргументы командной строки
     * @return неизменяемая конфигурация запуска
     * @throws CliValidationException если количество аргументов, формат или входные пути некорректны
     */
    public static CliConfig parse(String[] args) throws CliValidationException {
        if (args == null || args.length != 5) {
            throw new CliValidationException("error.args.count");
        }
        if (!"csv".equals(args[0]) && !"json".equals(args[0])) {
            throw new CliValidationException("error.args.format");
        }
        try {
            Path parking = Path.of(args[1]);
            Path properties = Path.of(args[2]);
            Path population = Path.of(args[3]);
            Path log = Path.of(args[4]);
            ensureReadable(parking);
            ensureReadable(properties);
            ensureReadable(population);
            return new CliConfig(args[0], parking, properties, population, log);
        } catch (RuntimeException ex) {
            throw new CliValidationException("error.file.unreadable", ex.getMessage());
        }
    }

    /**
     * Проверяет, что путь указывает на доступный для чтения обычный файл, и пробует его открыть.
     * Содержимое файла при этой проверке не разбирается.
     *
     * @param path проверяемый путь
     * @throws CliValidationException если файл отсутствует, недоступен или не может быть открыт
     */
    private static void ensureReadable(Path path) throws CliValidationException {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new CliValidationException("error.file.unreadable", path.toString());
        }
        try (var input = Files.newInputStream(path)) {
            // Проверка открытия позволяет сообщить о недоступном файле до отображения меню.
            input.readNBytes(0);
        } catch (IOException ex) {
            throw new CliValidationException("error.file.unreadable", path.toString());
        }
    }
}
