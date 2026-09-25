package com.marlus.financas.dataio.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Porta de exportação/importação de uma entidade em CSV (seção 9): cada módulo registra um
 * adaptador concreto (ex.: {@code AccountCsvHandler}) descobrível pelo Spring e agrupado pelo
 * {@link com.marlus.financas.dataio.service.DataIoRegistry}.
 */
public interface EntityCsvHandler {

    /** Nome do arquivo dentro do zip, ex. {@code "accounts.csv"}. */
    String fileName();

    List<String> header();

    /** Linhas do usuário, na mesma ordem do cabeçalho. */
    List<List<String>> exportRows(UUID userId);

    /**
     * Valida e, se {@code apply=true}, aplica (upsert por UUID) uma linha. Nunca deve lançar
     * exceção para erro de dado do usuário — erros de validação viram {@link RowResult#error}.
     */
    RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply);

    /** Remove todos os dados desta entidade para o usuário — usado no modo "Substituir tudo". */
    void deleteAllForUser(UUID userId);
}
