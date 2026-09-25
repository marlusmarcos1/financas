package com.marlus.financas.allocation.web;

import com.marlus.financas.investment.domain.InvestmentAssetClass;
import java.util.List;

public record AllocationSummaryResponse(List<AllocationComparisonResponse> allocations, InvestmentAssetClass nextContributionSuggestion) {
}
