package ru.ibs.diploma.datamanagement;

import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.data.PropertyEntry;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * Управляет ленивой загрузкой и хранит успешно прочитанные источники в пределах
 * одного запуска приложения.
 * <p>Повторный запрос уже загруженного источника не перечитывает файл. При
 * запросе новых источников к ранее загруженным данным добавляются только недостающие.
 */
public final class DataLoadingSession {
    private final CliConfig config;
    private final DataLoadingService loader;
    private final EnumSet<DataSource> loaded = EnumSet.noneOf(DataSource.class);

    private List<ParkingViolation> parkingRows = List.of();
    private List<PopulationEntry> populationRows = List.of();
    private List<PropertyEntry> propertyRows = List.of();

    /**
     * Создаёт сессию для заданной конфигурации и сервиса загрузки.
     *
     * @param config конфигурация путей и формата исходных данных
     * @param loader компонент фактического чтения файлов
     */
    public DataLoadingSession(CliConfig config, DataLoadingService loader) {
        this.config = config;
        this.loader = loader;
    }

    /**
     * Возвращает объединённый снимок данных, дозагружая только ещё не читавшиеся источники.
     * Метод синхронизирован, чтобы параллельные запросы не запускали повторную загрузку одного источника.
     *
     * @param requested требуемые для текущей операции источники
     * @return снимок всех источников, успешно загруженных в этой сессии
     * @throws IOException если чтение любого недостающего источника завершилось ошибкой
     * @throws IllegalArgumentException если набор источников равен {@code null}
     */
    public synchronized LoadedData load(EnumSet<DataSource> requested) throws IOException {
        if (requested == null) {
            throw new IllegalArgumentException("Не задан набор источников");
        }

        EnumSet<DataSource> missing = EnumSet.noneOf(DataSource.class);
        missing.addAll(requested);
        missing.removeAll(loaded);

        if (!missing.isEmpty()) {
            LoadedData next = loader.load(config, missing);
            if (missing.contains(DataSource.PARKING)) {
                parkingRows = next.getParkingViolations();
            }
            if (missing.contains(DataSource.POPULATION)) {
                populationRows = next.getPopulationEntries();
            }
            if (missing.contains(DataSource.PROPERTIES)) {
                propertyRows = next.getPropertyEntries();
            }
            loaded.addAll(missing);
        }
        return new LoadedData(parkingRows, propertyRows, populationRows);
    }
}
