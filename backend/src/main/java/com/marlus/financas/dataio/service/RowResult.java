package com.marlus.financas.dataio.service;

public record RowResult(RowOutcome outcome, int line, String errorMessage) {

    public static RowResult created(int line) {
        return new RowResult(RowOutcome.CREATED, line, null);
    }

    public static RowResult updated(int line) {
        return new RowResult(RowOutcome.UPDATED, line, null);
    }

    public static RowResult error(int line, String message) {
        return new RowResult(RowOutcome.ERROR, line, message);
    }
}
