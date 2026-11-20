package ru.ibs.diploma.application;

import ru.ibs.diploma.data.AdditionalFeatureResult;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.datamanagement.DataLoadingSession;
import ru.ibs.diploma.datamanagement.DataSource;
import ru.ibs.diploma.processor.AnalyticsService;
import ru.ibs.diploma.processor.ComputationCache;
import ru.ibs.diploma.processor.PropertyAverageStrategy;
import ru.ibs.diploma.processor.PropertyMetric;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Прикладной фасад аналитических команд.
 * <p>
 * Определяет, какие источники необходимы для конкретного показателя, получает
 * их через {@link DataLoadingSession}, создаёт аналитический сервис для набора
 * источников и повторно использует его при последующих вызовах.
 */
public final class AnalyticsCommandService {
    private final DataLoadingSession session;
    private final List<PropertyAverageStrategy> strategies;
    private final ComputationCache cache;
    private final Map<Set<DataSource>, AnalyticsService> services = new HashMap<>();

    /**
     * Создаёт фасад аналитических команд с сессией загрузки, стратегиями и кешем.
     *
     * @param session сессия ленивой загрузки исходных данных
     * @param strategies доступные стратегии расчёта средних показателей недвижимости
     * @param cache общий кеш результатов аналитики
     */
    public AnalyticsCommandService(DataLoadingSession session, List<PropertyAverageStrategy> strategies, ComputationCache cache) {
        this.session = session;
        this.strategies = List.copyOf(strategies);
        this.cache = cache;
    }

    /**
     * Возвращает аналитический сервис для набора источников, создавая его при первом запросе.
     * Сессия загрузки может дополнить запрошенный набор ранее загруженными источниками.
     *
     * @param sources источники, необходимые текущей команде
     * @return сервис, построенный на данных, доступных в сессии
     * @throws IOException если один из требуемых файлов не удалось прочитать
     */
    private AnalyticsService useAnalyticsCommandService(EnumSet<DataSource> sources) throws IOException {
        Set<DataSource> key = Set.copyOf(sources);
        AnalyticsService existing = services.get(key);
        if (existing != null) {
            return existing;
        }
        LoadedData data = session.load(sources);
        AnalyticsService service = new AnalyticsService(data, strategies, cache);
        services.put(key, service);
        return service;
    }

    /**
     * Возвращает суммарную численность населения по всем ZIP-кодам.
     *
     * @return сумма значений населения
     * @throws IOException если файл населения не удалось прочитать
     */
    public synchronized long totalPopulation() throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.POPULATION)).totalPopulation();
    }

    /**
     * Возвращает сумму парковочных штрафов на одного жителя по ZIP-кодам.
     * Для этой операции загружаются парковочные записи и данные о населении;
     * в расчёте штрафов аналитический слой учитывает только штат регистрации PA.
     *
     * @return отсортированная карта ZIP-кодов и суммы штрафов на жителя
     * @throws IOException если один из необходимых файлов не удалось прочитать
     */
    public synchronized NavigableMap<String, BigDecimal> finesPerCapita() throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PARKING, DataSource.POPULATION)).finesPerCapita();
    }

    /**
     * Рассчитывает среднее значение выбранной характеристики недвижимости для ZIP-кода.
     *
     * @param zip исходное значение ZIP-кода; будет нормализовано до пяти цифр
     * @param metric показатель недвижимости: рыночная стоимость или жилая площадь
     * @return среднее значение показателя; при некорректном или отсутствующем ZIP возвращается ноль
     * @throws IOException если файл недвижимости не удалось прочитать
     */
    public synchronized BigDecimal averageProperty(String zip, PropertyMetric metric) throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PROPERTIES)).averageProperty(zip, metric);
    }

    /**
     * Рассчитывает совокупную рыночную стоимость недвижимости на одного жителя ZIP-кода.
     *
     * @param zip исходное значение ZIP-кода
     * @return сумма валидных рыночных стоимостей, делённая на население; при недостатке данных — ноль
     * @throws IOException если файл недвижимости или населения не удалось прочитать
     */
    public synchronized BigDecimal marketValuePerCapita(String zip) throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PROPERTIES, DataSource.POPULATION)).marketValuePerCapita(zip);
    }

    /**
     * Выполняет дополнительный анализ нарушений и средней стоимости недвижимости по ZIP-кодам.
     * Для анализа загружаются все три источника данных.
     *
     * @return метрики по ZIP-кодам и коэффициент ранговой корреляции Спирмена, если он определён
     * @throws IOException если хотя бы один из необходимых файлов не удалось прочитать
     */
    public synchronized AdditionalFeatureResult additionalFeature() throws IOException {
        return useAnalyticsCommandService(EnumSet.allOf(DataSource.class)).additionalFeature();
    }
}
