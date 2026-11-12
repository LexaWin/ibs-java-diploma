package ru.ibs.diploma.ui;

import org.springframework.stereotype.Component;
import ru.ibs.diploma.application.AnalyticsCommandService;
import ru.ibs.diploma.data.AdditionalFeatureResult;
import ru.ibs.diploma.exception.DataReadException;
import ru.ibs.diploma.processor.PropertyMetric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Главное меню приложения.
 */
@Component
public class MenuController {
    private final LocalizationService messages;

    public MenuController(LocalizationService messages) {
        this.messages = messages;
    }

    private static void printMap(Map<String, BigDecimal> values) {
        values.forEach((zip, value) -> System.out.println(zip + " " + value.toPlainString()));
    }

    private void printZipAverage(Scanner scanner, AnalyticsCommandService analytics, PropertyMetric metric) throws IOException {
        System.out.print(messages.text("prompt.zip"));
        if (scanner.hasNextLine()) {
            System.out.println(messages.text("data.processing"));
            String zip = scanner.nextLine().trim();
            System.out.println(analytics.averageProperty(zip, metric).toBigInteger());
        }
    }

    private void printMarketPerCapita(Scanner scanner, AnalyticsCommandService analytics) throws IOException {
        System.out.print(messages.text("prompt.zip"));
        if (scanner.hasNextLine()) {
            System.out.println(messages.text("data.processing"));
            System.out.println(analytics.marketValuePerCapita(scanner.nextLine().trim()).toBigInteger());
        }
    }

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
     * Показывает меню и обрабатывает выбор пользователя.
     */
    public void run(AnalyticsCommandService analytics) {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMenu();
                System.out.print(messages.text("prompt.command"));
                if (!scanner.hasNextLine()) break;
                String raw = scanner.nextLine().trim();
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

                    System.err.println(messages.text(ex.messageKey(), ex.arguments()));
                } catch (IOException ex) {
                    System.err.println(messages.text("error.load", ex.getMessage()));
                }

                System.out.println();
            }
        }
    }

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
