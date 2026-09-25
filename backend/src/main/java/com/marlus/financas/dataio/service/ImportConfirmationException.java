package com.marlus.financas.dataio.service;

/** Modo "Substituir tudo" exige que o usuário digite a palavra "SUBSTITUIR" para confirmar. */
public class ImportConfirmationException extends RuntimeException {

    public ImportConfirmationException(String message) {
        super(message);
    }
}
