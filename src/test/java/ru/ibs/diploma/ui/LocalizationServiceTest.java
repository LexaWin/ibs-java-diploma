package ru.ibs.diploma.ui;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationServiceTest {
    @Test
    void selectsEnglishAndRussianBundles() {
        assertEquals("0. Exit", new LocalizationService(Locale.ENGLISH).text("menu.0"));
        assertEquals("0. Выход", new LocalizationService(Locale.forLanguageTag("ru")).text("menu.0"));
    }

    @Test
    void unsupportedLocaleUsesBaseEnglishBundleNotJvmDefault() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ru"));
            assertEquals("1. Total Population", new LocalizationService(Locale.GERMANY).text("menu.1"));
        } finally {
            Locale.setDefault(old);
        }
    }
}
