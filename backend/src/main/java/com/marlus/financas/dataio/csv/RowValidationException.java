package com.marlus.financas.dataio.csv;

/** Erro de dado do usuário numa linha de CSV — nunca deve propagar além de {@code importRow}. */
public class RowValidationException extends RuntimeException {

    public RowValidationException(String message) {
        super(message);
    }
}
