package com.marlus.financas.common;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/** Centraliza a geração de UUID v7 (ordenável por tempo) usados como PK em todas as tabelas. */
public final class UuidV7Generator {

    private UuidV7Generator() {
    }

    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
