package com.marlus.financas.dataio;

import static org.assertj.core.api.Assertions.assertThat;

import com.marlus.financas.dataio.csv.CsvReader;
import com.marlus.financas.dataio.csv.CsvWriter;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvRoundTripTest {

    @Test
    void quotesSemicolonsAndAccentsSurviveARoundTrip() {
        List<String> header = List.of("id", "description", "notes");
        List<List<String>> rows = List.of(
                List.of("1", "Compra; parcelada", "Contém \"aspas\" e ç, ã, é"),
                List.of("2", "Linha\ncom quebra", "Simples"));

        String csv = CsvWriter.write(header, rows);
        List<LinkedHashMap<String, String>> parsed = CsvReader.readAsMaps(csv);

        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0).get("description")).isEqualTo("Compra; parcelada");
        assertThat(parsed.get(0).get("notes")).isEqualTo("Contém \"aspas\" e ç, ã, é");
        assertThat(parsed.get(1).get("description")).isEqualTo("Linha\ncom quebra");
    }

    @Test
    void emptyFieldsRoundTripAsEmptyStrings() {
        List<String> header = List.of("id", "optional");
        List<List<String>> rows = List.of(List.of("1", ""));

        String csv = CsvWriter.write(header, rows);
        List<LinkedHashMap<String, String>> parsed = CsvReader.readAsMaps(csv);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).get("optional")).isEmpty();
    }

    @Test
    void onlyHeaderProducesNoDataRows() {
        List<String> header = List.of("id", "name");
        String csv = CsvWriter.write(header, List.of());

        List<LinkedHashMap<String, String>> parsed = CsvReader.readAsMaps(csv);

        assertThat(parsed).isEmpty();
    }
}
