package ru.ibs.diploma.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ZipCodeTest {
    @Test
    void normalizesLongZipToFiveDigits() {
        assertEquals("19104", ZipCode.normalize("191047632"));
    }

    @Test
    void rejectsInvalidPrefix() {
        assertNull(ZipCode.normalize("19A04"));
    }
}
