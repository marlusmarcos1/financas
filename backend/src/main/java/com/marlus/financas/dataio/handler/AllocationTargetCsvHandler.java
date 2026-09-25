package com.marlus.financas.dataio.handler;

import com.marlus.financas.allocation.domain.AllocationTarget;
import com.marlus.financas.allocation.repository.AllocationTargetRepository;
import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AllocationTargetCsvHandler implements EntityCsvHandler {

    private final AllocationTargetRepository allocationTargetRepository;

    public AllocationTargetCsvHandler(AllocationTargetRepository allocationTargetRepository) {
        this.allocationTargetRepository = allocationTargetRepository;
    }

    @Override
    public String fileName() {
        return "allocation_targets.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "purpose", "asset_class", "target_percent");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return allocationTargetRepository.findAllByUserId(userId).stream()
                .map(a -> List.of(
                        a.getId().toString(), a.getPurpose().name(), a.getAssetClass().name(),
                        CsvFieldParser.required(a.getTargetPercent())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            InvestmentPurpose purpose = CsvFieldParser.requiredEnum(row, "purpose", InvestmentPurpose.class);
            InvestmentAssetClass assetClass = CsvFieldParser.requiredEnum(row, "asset_class", InvestmentAssetClass.class);
            BigDecimal targetPercent = CsvFieldParser.requiredDecimal(row, "target_percent");

            var existing = allocationTargetRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                AllocationTarget target = existing.get();
                if (!Objects.equals(target.getPurpose(), purpose) || !Objects.equals(target.getAssetClass(), assetClass)) {
                    return RowResult.error(
                            line, "finalidade e classe de um alvo de alocação existente não podem ser alteradas por"
                                    + " importação");
                }
                if (apply) {
                    target.setTargetPercent(targetPercent);
                    allocationTargetRepository.save(target);
                }
                return RowResult.updated(line);
            }

            if (allocationTargetRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                allocationTargetRepository.save(new AllocationTarget(id, userId, purpose, assetClass, targetPercent));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        allocationTargetRepository.deleteAllByUserId(userId);
    }
}
