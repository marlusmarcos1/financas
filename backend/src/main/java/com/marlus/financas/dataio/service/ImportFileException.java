package com.marlus.financas.dataio.service;

/** Arquivo zip inválido, manifesto ausente/corrompido, checksum não confere ou versão de esquema incompatível. */
public class ImportFileException extends RuntimeException {

    public ImportFileException(String message) {
        super(message);
    }
}
