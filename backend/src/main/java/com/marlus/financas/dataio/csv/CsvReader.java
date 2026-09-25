package com.marlus.financas.dataio.csv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Lê CSV no mesmo formato que {@link CsvWriter} escreve (delimitador {@code ;}, aspas RFC 4180). */
public final class CsvReader {

    private static final char DELIMITER = ';';

    private CsvReader() {
    }

    /** Retorna as linhas já como mapa coluna→valor, usando o cabeçalho (primeira linha) como chaves. */
    public static List<LinkedHashMap<String, String>> readAsMaps(String content) {
        List<List<String>> rows = readRows(content);
        List<LinkedHashMap<String, String>> result = new ArrayList<>();
        if (rows.isEmpty()) {
            return result;
        }
        List<String> header = rows.get(0);
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for (int col = 0; col < header.size(); col++) {
                map.put(header.get(col), col < row.size() ? row.get(col) : "");
            }
            result.add(map);
        }
        return result;
    }

    public static List<List<String>> readRows(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean rowHasContent = false;
        int i = 0;
        int length = content.length();

        while (i < length) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < length && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                    continue;
                }
                field.append(c);
                i++;
                continue;
            }

            if (c == '"') {
                inQuotes = true;
                rowHasContent = true;
                i++;
            } else if (c == DELIMITER) {
                current.add(field.toString());
                field.setLength(0);
                rowHasContent = true;
                i++;
            } else if (c == '\r') {
                i++;
            } else if (c == '\n') {
                current.add(field.toString());
                field.setLength(0);
                if (rowHasContent || current.size() > 1) {
                    rows.add(current);
                }
                current = new ArrayList<>();
                rowHasContent = false;
                i++;
            } else {
                field.append(c);
                rowHasContent = true;
                i++;
            }
        }
        if (rowHasContent || !field.isEmpty() || current.size() > 1) {
            current.add(field.toString());
            rows.add(current);
        }
        return rows;
    }
}
