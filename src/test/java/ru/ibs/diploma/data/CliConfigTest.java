package ru.ibs.diploma.data;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.exception.CliValidationException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliConfigTest {
    @TempDir
    Path temp;

    @Test
    void validatesExactlyFiveArgumentsAndAllReadableInputFiles() throws Exception {
        Path parking = Files.writeString(temp.resolve("parking.csv"), "не разбираемый текст");
        Path properties = Files.writeString(temp.resolve("properties.csv"), "тоже не разбираемый текст");
        Path population = Files.writeString(temp.resolve("population.txt"), "некорректная строка");
        assertEquals(
                "csv",
                CliConfig.parse(
                        new String[]{"csv", parking.toString(), properties.toString(), population.toString(), "app.log"}
                ).getParkingFormat()
        );
        assertEquals(
                "error.args.count",
                assertThrows(CliValidationException.class, () -> CliConfig.parse(new String[]{"csv"})).messageKey()
        );
        assertEquals(
                "error.args.format",
                assertThrows(
                        CliValidationException.class,
                        () -> CliConfig.parse(
                                new String[]{
                                        "CSV",
                                        parking.toString(),
                                        properties.toString(),
                                        population.toString(),
                                        "app.log"
                                }
                        )
                ).messageKey()
        );
        assertThrows(
                CliValidationException.class,
                () -> CliConfig.parse(
                        new String[]{
                                "csv",
                                parking.toString(),
                                properties.toString(),
                                temp.resolve("missing.txt").toString(),
                                "app.log"
                        }
                )
        );
    }
}
