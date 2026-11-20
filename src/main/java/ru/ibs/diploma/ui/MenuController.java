package ru.ibs.diploma.ui;

import org.springframework.stereotype.Component;
import ru.ibs.diploma.application.AnalyticsCommandService;
import ru.ibs.diploma.data.AdditionalFeatureResult;
import ru.ibs.diploma.exception.DataReadException;
import ru.ibs.diploma.logging.ApplicationLogger;
import ru.ibs.diploma.processor.PropertyMetric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Реализует интерактивное консольное меню и форматирует результаты аналитики.
 * <p>Контроллер отвечает за ввод, вывод, журналирование пользовательских команд
 * и локализацию сообщений; сами расчёты делегируются {@link AnalyticsCommandService}.
 */
@Component
public class MenuController {
    private final LocalizationService messages;
    private final ApplicationLogger logger;

    /**
     * Создаёт контроллер меню с локализацией и журналом событий.
     *
     * @param messages сервис локализованных пользовательских сообщений
     * @param logger журнал пользовательских команд и ошибок
     */
    public MenuController(LocalizationService messages, ApplicationLogger logger) {
        this.messages = messages;
        this.logger = logger;
    }

    /**
     * Выводит карту аналитических значений, по одной строке на ZIP-код.
     *
     * @param values отображение ZIP-кода и рассчитанного десятичного значения
     */
    private static void printMap(Map<String, BigDecimal> values) {
        values.forEach((zip, value) -> System.out.println(zip + " " + value.toPlainString()));
    }

    /**
     * Запрашивает у пользователя ZIP-код и выводит среднее значение выбранной метрики недвижимости.
     *
     * @param scanner источник пользовательского ввода
     * @param analytics фасад аналитических команд
     * @param metric показатель недвижимости для расчёта
     * @throws IOException если данные недвижимости не удалось загрузить
     */
    private void printZipAverage(Scanner scanner, AnalyticsCommandService analytics, PropertyMetric metric) throws IOException {
        System.out.print(messages.text("prompt.zip"));
        if (scanner.hasNextLine()) {
            System.out.println(messages.text("data.processing"));
            String zip = scanner.nextLine().trim();
            logger.log("ZIP " + zip);
            System.out.println(analytics.averageProperty(zip, metric).toBigInteger());
        }
    }

    /**
     * Запрашивает ZIP-код и выводит совокупную стоимость недвижимости на одного жителя.
     *
     * @param scanner источник пользовательского ввода
     * @param analytics фасад аналитических команд
     * @throws IOException если данные недвижимости или населения не удалось загрузить
     */
    private void printMarketPerCapita(Scanner scanner, AnalyticsCommandService analytics) throws IOException {
        System.out.print(messages.text("prompt.zip"));
        if (scanner.hasNextLine()) {
            String zip = scanner.nextLine().trim();
            logger.log("ZIP " + zip);
            System.out.println(messages.text("data.processing"));
            System.out.println(analytics.marketValuePerCapita(zip).toBigInteger());
        }
    }

    /**
     * Выводит таблицу дополнительного анализа с псевдографическими границами,
     * а затем отображает коэффициент Спирмена либо причину его неопределённости.
     *
     * @param result результат сравнения частоты нарушений и стоимости недвижимости
     */
    private void printAdditionalFeature(AdditionalFeatureResult result) {
        String[] headers = messages.text("feature.header")
                .split("\\s{2,}", -1);

        List<String[]> rows = new ArrayList<>();

        result.getByZip().forEach((zip, metrics) -> rows.add(new String[]{
                zip,
                metrics.getViolationsPer1000Residents()
                        .setScale(4, RoundingMode.HALF_UP)
                        .toPlainString(),
                metrics.getAverageMarketValue()
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString()
        }));

        int[] widths = new int[3];

        for (int i = 0; i < widths.length; i++) {
            widths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < widths.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        // Верхняя граница таблицы.
        StringBuilder top = new StringBuilder("┌");

        for (int i = 0; i < widths.length; i++) {
            top.append("─".repeat(widths[i] + 2));
            top.append(i == widths.length - 1 ? "┐" : "┬");
        }

        // Разделитель заголовка и данных.
        StringBuilder middle = new StringBuilder("├");

        for (int i = 0; i < widths.length; i++) {
            middle.append("─".repeat(widths[i] + 2));
            middle.append(i == widths.length - 1 ? "┤" : "┼");
        }

        // Нижняя граница таблицы.
        StringBuilder bottom = new StringBuilder("└");

        for (int i = 0; i < widths.length; i++) {
            bottom.append("─".repeat(widths[i] + 2));
            bottom.append(i == widths.length - 1 ? "┘" : "┴");
        }

        System.out.println(top);

        // Заголовок.
        printTableRow(headers, widths);

        System.out.println(middle);

        // Данные.
        for (String[] row : rows) {
            printTableRow(row, widths);
        }

        System.out.println(bottom);

        // Корреляция.
        if (result.getSpearmanCorrelation().isPresent()) {
            BigDecimal correlation =
                    result.getSpearmanCorrelation().orElseThrow();

            System.out.println(messages.text(
                    "feature.correlation",
                    correlation.toPlainString(),
                    result.getSampleSize()
            ));
        } else {
            System.out.println(messages.text(
                    "feature.correlation.undefined",
                    result.getSampleSize()
            ));
        }
    }

    /**
     * Выводит одну строку таблицы с правым выравниванием значений в заданных ширинах.
     *
     * @param values текстовые значения ячеек
     * @param widths ширина соответствующей колонки без учёта внутренних пробелов
     */
    private static void printTableRow(String[] values, int[] widths) {
        StringBuilder row = new StringBuilder("│");

        for (int i = 0; i < values.length; i++) {
            row.append(" ")
                    .append(String.format("%" + widths[i] + "s", values[i]))
                    .append(" │");
        }

        System.out.println(row);
    }

    /**
     * Запускает цикл меню, разбирает команды пользователя и делегирует расчёты прикладному сервису.
     * Ошибки чтения данных локализуются и выводятся без аварийного завершения меню.
     *
     * @param analytics фасад для выполнения аналитических команд
     */
    public void run(AnalyticsCommandService analytics) {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMenu();
                System.out.print(messages.text("prompt.command"));
                if (!scanner.hasNextLine()) break;
                String raw = scanner.nextLine().trim();
                logger.log("COMMAND " + raw);
                int command;
                try {
                    command = Integer.parseInt(raw);
                } catch (NumberFormatException ex) {
                    System.out.println(messages.text("error.command"));
                    continue;
                }
                try {
                    switch (command) {
                        case 0 -> {
                            System.out.println(messages.text("message.goodbye"));
                            running = false;
                        }
                        case 1 -> System.out.println(analytics.totalPopulation());
                        case 2 -> printMap(analytics.finesPerCapita());
                        case 3 -> printZipAverage(scanner, analytics, PropertyMetric.MARKET_VALUE);
                        case 4 -> printZipAverage(scanner, analytics, PropertyMetric.LIVABLE_AREA);
                        case 5 -> printMarketPerCapita(scanner, analytics);
                        case 6 -> {
                            System.out.println(messages.text("feature.description"));
                            System.out.println(messages.text("data.processing"));
                            printAdditionalFeature(analytics.additionalFeature());
                        }
                        default -> System.out.println(messages.text("error.command"));
                    }
                } catch (DataReadException ex) {
                    logger.log("DATA_READING_ERROR " + ex.getMessage());
                    System.err.println(messages.text(ex.messageKey(), ex.arguments()));
                } catch (IOException ex) {
                    logger.log("DATA_LOADING_ERROR " + ex.getMessage());
                    System.err.println(messages.text("error.load", ex.getMessage()));
                }

                System.out.println();
            }
        }
    }

    /**
     * Выводит заголовок, все доступные команды и подсказку выхода на выбранном языке.
     */
    private void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("       " + messages.text("menu.title"));
        System.out.println("========================================");
        for (int i = 1; i <= 6; i++) {
            System.out.println(messages.text("menu." + i));
        }
        System.out.println();
        System.out.println(messages.text("menu.0"));
        System.out.println();
    }
}
