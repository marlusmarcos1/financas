package com.marlus.financas.dataio.csv;

import java.util.List;

/**
 * Escreve CSV no formato pedido pela seção 9: UTF-8, delimitador {@code ;}, cabeçalho na
 * primeira linha, aspas para campos com {@code ;}/quebra de linha/aspas (aspas duplicadas por
 * escape), sem BOM.
 */
public final class CsvWriter {

    private static final char DELIMITER = ';';

    private CsvWriter() {
    }

    public static String write(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        writeRow(sb, header);
        for (List<String> row : rows) {
            writeRow(sb, row);
        }
        return sb.toString();
    }

    private static void writeRow(StringBuilder sb, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(DELIMITER);
            }
            sb.append(escape(values.get(i)));
        }
        sb.append("\r\n");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.indexOf(DELIMITER) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
