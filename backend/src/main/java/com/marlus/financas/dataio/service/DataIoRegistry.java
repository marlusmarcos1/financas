package com.marlus.financas.dataio.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Agrupa todos os {@link EntityCsvHandler} descobertos pelo Spring e define a ordem de
 * dependência entre entidades (seção 9): contas/cartões/categorias → receitas → recorrências →
 * parcelamentos → faturas → lançamentos → dízimo → investimentos → metas → configurações.
 * Import usa esta ordem; "Substituir tudo" apaga na ordem inversa.
 */
@Component
public class DataIoRegistry {

    private static final List<String> FILE_ORDER = List.of(
            "accounts.csv",
            "credit_cards.csv",
            "categories.csv",
            "budgets.csv",
            "income_sources.csv",
            "income_entries.csv",
            "recurring_rules.csv",
            "installment_plans.csv",
            "invoices.csv",
            "transactions.csv",
            "tithe_ledger.csv",
            "investment_assets.csv",
            "investment_transactions.csv",
            "allocation_targets.csv",
            "retirement_plans.csv",
            "goals.csv",
            "goal_contributions.csv",
            "settings.csv");

    private final Map<String, EntityCsvHandler> handlersByFileName = new LinkedHashMap<>();

    public DataIoRegistry(List<EntityCsvHandler> handlers) {
        Map<String, EntityCsvHandler> byName = new LinkedHashMap<>();
        for (EntityCsvHandler handler : handlers) {
            byName.put(handler.fileName(), handler);
        }
        for (String fileName : FILE_ORDER) {
            EntityCsvHandler handler = byName.get(fileName);
            if (handler != null) {
                handlersByFileName.put(fileName, handler);
            }
        }
    }

    /** Handlers na ordem de dependência (para exportar e para importar). */
    public List<EntityCsvHandler> handlersInDependencyOrder() {
        return List.copyOf(handlersByFileName.values());
    }

    /** Ordem inversa — para apagar dados respeitando FKs no modo "Substituir tudo". */
    public List<EntityCsvHandler> handlersInReverseDependencyOrder() {
        List<EntityCsvHandler> ordered = new java.util.ArrayList<>(handlersInDependencyOrder());
        java.util.Collections.reverse(ordered);
        return ordered;
    }

    public EntityCsvHandler byFileName(String fileName) {
        return handlersByFileName.get(fileName);
    }
}
