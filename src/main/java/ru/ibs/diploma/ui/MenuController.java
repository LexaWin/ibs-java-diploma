package ru.ibs.diploma.ui;

import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.Scanner;

/** Главное меню приложения. */
@Component
public class MenuController {
    private final LocalizationService messages;

    public MenuController(LocalizationService messages) {
        this.messages = messages;
    }

    /** Показывает меню и обрабатывает выбор пользователя. */
    public void run() {
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

                switch (command) {
                    case 0 -> {
                        System.out.println(messages.text("message.goodbye"));
                        running = false;
                    }
                    case 1, 2, 3, 4, 5, 6 -> System.out.println("To be implemented...");
                    default -> System.out.println(messages.text("error.command"));
                }
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
