package com.marlus.financas.investment.web;

import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record InvestmentAssetRequest(
        @NotBlank(message = "Informe o ticker.") String ticker,
        @NotBlank(message = "Informe o nome.") String name,
        @NotNull(message = "Informe a classe do ativo.") InvestmentAssetClass assetClass,
        String subclass,
        String indexer,
        LocalDate maturityDate,
        @NotNull(message = "Informe a finalidade.") InvestmentPurpose purpose) {
}
