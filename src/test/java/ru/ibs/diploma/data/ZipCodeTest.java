package ru.ibs.diploma.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ZipCodeTest {
    @Test
    void usesOnlyTheFirstFiveDigits() {
        assertEquals("19104", ZipCode.normalize("191047632"));
        assertEquals("19104", ZipCode.normalize("191042"));
    }

    @Test
    void rejectsMissingOrNonNumericPrefix() {
        assertNull(ZipCode.normalize(""));
        assertNull(ZipCode.normalize("19A04"));
        assertNull(ZipCode.normalize(null));
    }
}
