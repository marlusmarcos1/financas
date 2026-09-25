package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.investment.domain.InvestmentAsset;
import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import com.marlus.financas.investment.repository.InvestmentAssetRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InvestmentAssetCsvHandler implements EntityCsvHandler {

    private final InvestmentAssetRepository investmentAssetRepository;

    public InvestmentAssetCsvHandler(InvestmentAssetRepository investmentAssetRepository) {
        this.investmentAssetRepository = investmentAssetRepository;
    }

    @Override
    public String fileName() {
        return "investment_assets.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id",
                "ticker",
                "name",
                "asset_class",
                "subclass",
                "indexer",
                "maturity_date",
                "current_price",
                "price_updated_at",
                "purpose",
                "archived");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return investmentAssetRepository.findAllByUserIdOrderByTickerAsc(userId).stream()
                .map(a -> List.of(
                        a.getId().toString(),
                        a.getTicker(),
                        a.getName(),
                        a.getAssetClass().name(),
                        CsvFieldParser.opt(a.getSubclass()),
                        CsvFieldParser.opt(a.getIndexer()),
                        CsvFieldParser.opt(a.getMaturityDate()),
                        CsvFieldParser.opt(a.getCurrentPrice()),
                        CsvFieldParser.opt(a.getPriceUpdatedAt()),
                        a.getPurpose().name(),
                        CsvFieldParser.bool(a.isArchived())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String ticker = CsvFieldParser.requiredText(row, "ticker");
            String name = CsvFieldParser.requiredText(row, "name");
            InvestmentAssetClass assetClass = CsvFieldParser.requiredEnum(row, "asset_class", InvestmentAssetClass.class);
            String subclass = CsvFieldParser.optionalText(row, "subclass");
            String indexer = CsvFieldParser.optionalText(row, "indexer");
            LocalDate maturityDate = CsvFieldParser.optionalDate(row, "maturity_date");
            BigDecimal currentPrice = CsvFieldParser.optionalDecimal(row, "current_price");
            String priceUpdatedAtText = CsvFieldParser.optionalText(row, "price_updated_at");
            Instant priceUpdatedAt = priceUpdatedAtText == null ? null : parseInstant(priceUpdatedAtText);
            InvestmentPurpose purpose = CsvFieldParser.requiredEnum(row, "purpose", InvestmentPurpose.class);
            boolean archived = CsvFieldParser.requiredBoolean(row, "archived");

            var existing = investmentAssetRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                InvestmentAsset asset = existing.get();
                if (apply) {
                    asset.setTicker(ticker);
                    asset.setName(name);
                    asset.setAssetClass(assetClass);
                    asset.setSubclass(subclass);
                    asset.setIndexer(indexer);
                    asset.setMaturityDate(maturityDate);
                    asset.updatePrice(currentPrice, priceUpdatedAt);
                    asset.setPurpose(purpose);
                    if (archived) {
                        asset.archive();
                    } else {
                        asset.unarchive();
                    }
                    investmentAssetRepository.save(asset);
                }
                return RowResult.updated(line);
            }

            if (investmentAssetRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                InvestmentAsset asset = new InvestmentAsset(
                        id, userId, ticker, name, assetClass, subclass, indexer, maturityDate, purpose);
                if (currentPrice != null) {
                    asset.updatePrice(currentPrice, priceUpdatedAt);
                }
                if (archived) {
                    asset.archive();
                }
                investmentAssetRepository.save(asset);
            }
            return RowResult.created(line);
        } catch (RowValidationException | java.time.format.DateTimeParseException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new com.marlus.financas.dataio.csv.RowValidationException(
                    "campo \"price_updated_at\" deve estar em formato ISO-8601: " + value);
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        investmentAssetRepository.deleteAllByUserId(userId);
    }
}
