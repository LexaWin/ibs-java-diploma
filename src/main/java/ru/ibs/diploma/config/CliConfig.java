package ru.ibs.diploma.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import ru.ibs.diploma.exception.CliValidationException;

/** Конфигурация командной строки */
public class CliConfig {
    private final String parkingFormat;
    private final Path parkingFile;
    private final Path propertiesFile;
    private final Path populationFile;
    private final Path logFile;

    private CliConfig(String parkingFormat, Path parkingFile, Path propertiesFile,
                      Path populationFile, Path logFile) {
        this.parkingFormat = parkingFormat;
        this.parkingFile = parkingFile;
        this.propertiesFile = propertiesFile;
        this.populationFile = populationFile;
        this.logFile = logFile;
    }

    public String getParkingFormat() { return parkingFormat; }
    public Path getParkingFile() { return parkingFile; }
    public Path getPropertiesFile() { return propertiesFile; }
    public Path getPopulationFile() { return populationFile; }
    public Path getLogFile() { return logFile; }

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
