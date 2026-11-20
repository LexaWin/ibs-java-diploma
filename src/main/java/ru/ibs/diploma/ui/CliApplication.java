package ru.ibs.diploma.ui;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.application.AnalyticsCommandService;
import ru.ibs.diploma.application.ExitCodeHolder;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.datamanagement.*;
import ru.ibs.diploma.exception.CliValidationException;
import ru.ibs.diploma.logging.ApplicationLogger;
import ru.ibs.diploma.processor.ComputationCache;
import ru.ibs.diploma.processor.PropertyAverageStrategy;

import java.util.Arrays;
import java.util.List;

/**
 * Адаптер запуска CLI-сценария в жизненном цикле Spring Boot.
 * <p>Проверяет аргументы, создаёт сессию загрузки и прикладной сервис, после чего
 * передаёт управление интерактивному меню. При ошибке CLI-валидации устанавливает
 * ненулевой код завершения.
 */
@Component
public class CliApplication implements ApplicationRunner {
    private final ExitCodeHolder exitCode;
    private final LocalizationService messages;
    private final MenuController menu;
    private final List<PropertyAverageStrategy> strategies;
    private final ComputationCache cache;
    private final DataLoadingService loader;
    private final ApplicationLogger logger;

    /**
     * Создаёт CLI-адаптер со всеми зависимостями, необходимыми для интерактивной работы.
     *
     * @param exitCode держатель кода завершения процесса
     * @param messages сервис локализации
     * @param menu контроллер интерактивного меню
     * @param strategies зарегистрированные стратегии средних показателей
     * @param cache кеш аналитических результатов
     * @param loader сервис чтения исходных файлов
     * @param logger журнал приложения
     */
    public CliApplication(ExitCodeHolder exitCode, LocalizationService messages, MenuController menu,
                          List<PropertyAverageStrategy> strategies, ComputationCache cache,
                          DataLoadingService loader, ApplicationLogger logger) {
        this.exitCode = exitCode;
        this.messages = messages;
        this.menu = menu;
        this.strategies = List.copyOf(strategies);
        this.cache = cache;
        this.loader = loader;
        this.logger = logger;
    }

    /**
     * Проверяет аргументы, открывает сессию загрузки и запускает меню.
     * Файлы проверяются на доступность в процессе разбора конфигурации, но читаются
     * только тогда, когда пользователь выбирает требующую их аналитическую команду.
     *
     * @param arguments аргументы, разобранные Spring Boot
     */
    @Override
    public void run(ApplicationArguments arguments) {
        String[] args = arguments.getSourceArgs();
        try {
            logger.log("START args=" + Arrays.toString(args));
            CliConfig config = CliConfig.parse(args);
            DataLoadingSession session = loader.openSession(config);
            AnalyticsCommandService analytics = new AnalyticsCommandService(session, strategies, cache);
            menu.run(analytics);
        } catch (CliValidationException exception) {
            System.err.println(messages.text(exception.messageKey(), (Object) exception.arguments()));
            exitCode.set(2); // Код 2 - Ошибка аргументов командной строки
        }
    }
}
