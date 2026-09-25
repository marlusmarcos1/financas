package com.marlus.financas.allocation.web;

import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AllocationTargetRequest(
        @NotNull(message = "Informe a finalidade.") InvestmentPurpose purpose,
        @NotNull(message = "Informe a classe do ativo.") InvestmentAssetClass assetClass,
        @NotNull(message = "Informe o percentual alvo.") @DecimalMin(value = "0", message = "Entre 0 e 100.")
                @DecimalMax(value = "100", message = "Entre 0 e 100.") BigDecimal targetPercent) {
}
