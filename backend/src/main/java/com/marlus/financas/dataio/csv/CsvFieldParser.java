package com.marlus.financas.dataio.csv;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Leitura tipada de campos de uma linha de CSV (seção 9) com mensagens de erro em pt-BR.
 * Todo método lança {@link RowValidationException} para dado ausente/mal formatado — nunca
 * {@code NullPointerException}/{@code IllegalArgumentException} cru.
 */
public final class CsvFieldParser {

    private CsvFieldParser() {
    }

    private static String raw(Map<String, String> row, String field) {
        String value = row.get(field);
        return value == null ? "" : value.trim();
    }

    public static String requiredText(Map<String, String> row, String field) {
        String value = raw(row, field);
        if (value.isEmpty()) {
            throw new RowValidationException("campo \"" + field + "\" é obrigatório");
        }
        return value;
    }

    public static String optionalText(Map<String, String> row, String field) {
        String value = raw(row, field);
        return value.isEmpty() ? null : value;
    }

    public static UUID requiredUuid(Map<String, String> row, String field) {
        String value = requiredText(row, field);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new RowValidationException("campo \"" + field + "\" não é um UUID válido: " + value);
        }
    }

    public static UUID optionalUuid(Map<String, String> row, String field) {
        String value = raw(row, field);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new RowValidationException("campo \"" + field + "\" não é um UUID válido: " + value);
        }
    }

    public static BigDecimal requiredDecimal(Map<String, String> row, String field) {
        String value = requiredText(row, field);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new RowValidationException("campo \"" + field + "\" não é um número válido: " + value);
        }
    }

    public static BigDecimal optionalDecimal(Map<String, String> row, String field) {
        String value = raw(row, field);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new RowValidationException("campo \"" + field + "\" não é um número válido: " + value);
        }
    }

    public static LocalDate requiredDate(Map<String, String> row, String field) {
        String value = requiredText(row, field);
        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new RowValidationException("campo \"" + field + "\" deve estar no formato AAAA-MM-DD: " + value);
        }
    }

    public static LocalDate optionalDate(Map<String, String> row, String field) {
        String value = raw(row, field);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new RowValidationException("campo \"" + field + "\" deve estar no formato AAAA-MM-DD: " + value);
        }
    }

    public static short requiredShort(Map<String, String> row, String field) {
        String value = requiredText(row, field);
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException ex) {
            throw new RowValidationException("campo \"" + field + "\" não é um inteiro válido: " + value);
        }
    }

    public static Short optionalShort(Map<String, String> row, String field) {
        String value = raw(row, field);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException ex) {
            throw new RowValidationException("campo \"" + field + "\" não é um inteiro válido: " + value);
        }
    }

    public static boolean requiredBoolean(Map<String, String> row, String field) {
        String value = requiredText(row, field);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new RowValidationException("campo \"" + field + "\" deve ser \"true\" ou \"false\": " + value);
    }

    public static <E extends Enum<E>> E requiredEnum(Map<String, String> row, String field, Class<E> type) {
        String value = requiredText(row, field);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ex) {
            throw new RowValidationException("campo \"" + field + "\" tem valor inválido: " + value);
        }
    }

    public static String required(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    public static String opt(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    public static String opt(Object value) {
        return value == null ? "" : value.toString();
    }

    public static String bool(boolean value) {
        return Boolean.toString(value);
    }
}
