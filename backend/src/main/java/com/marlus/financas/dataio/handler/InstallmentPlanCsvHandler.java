package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.installment.domain.InstallmentPlan;
import com.marlus.financas.installment.repository.InstallmentPlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * {@link InstallmentPlan} não expõe setters (é imutável após criado, pois gera faturas/lançamentos
 * no momento da criação) — na importação, uma linha existente só é aceita se for idêntica à
 * armazenada; qualquer divergência é erro (exclua e recrie o parcelamento).
 */
@Component
public class InstallmentPlanCsvHandler implements EntityCsvHandler {

    private final InstallmentPlanRepository installmentPlanRepository;

    public InstallmentPlanCsvHandler(InstallmentPlanRepository installmentPlanRepository) {
        this.installmentPlanRepository = installmentPlanRepository;
    }

    @Override
    public String fileName() {
        return "installment_plans.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id",
                "card_id",
                "description",
                "purchase_date",
                "total_amount",
                "installment_count",
                "installment_amount",
                "first_installment_number",
                "first_invoice_month",
                "interest_rate_monthly",
                "category_id");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return installmentPlanRepository.findAllByUserIdOrderByPurchaseDateDesc(userId).stream()
                .map(p -> List.of(
                        p.getId().toString(),
                        p.getCardId().toString(),
                        p.getDescription(),
                        p.getPurchaseDate().toString(),
                        CsvFieldParser.required(p.getTotalAmount()),
                        String.valueOf(p.getInstallmentCount()),
                        CsvFieldParser.required(p.getInstallmentAmount()),
                        String.valueOf(p.getFirstInstallmentNumber()),
                        p.getFirstInvoiceMonth().toString(),
                        CsvFieldParser.opt(p.getInterestRateMonthly()),
                        CsvFieldParser.opt(p.getCategoryId())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            UUID cardId = CsvFieldParser.requiredUuid(row, "card_id");
            String description = CsvFieldParser.requiredText(row, "description");
            LocalDate purchaseDate = CsvFieldParser.requiredDate(row, "purchase_date");
            BigDecimal totalAmount = CsvFieldParser.requiredDecimal(row, "total_amount");
            short installmentCount = CsvFieldParser.requiredShort(row, "installment_count");
            BigDecimal installmentAmount = CsvFieldParser.requiredDecimal(row, "installment_amount");
            short firstInstallmentNumber = CsvFieldParser.requiredShort(row, "first_installment_number");
            YearMonth firstInvoiceMonth = YearMonth.parse(CsvFieldParser.requiredText(row, "first_invoice_month"));
            BigDecimal interestRateMonthly = CsvFieldParser.optionalDecimal(row, "interest_rate_monthly");
            UUID categoryId = CsvFieldParser.optionalUuid(row, "category_id");

            var existing = installmentPlanRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                InstallmentPlan plan = existing.get();
                boolean identical = Objects.equals(plan.getCardId(), cardId)
                        && Objects.equals(plan.getDescription(), description)
                        && Objects.equals(plan.getPurchaseDate(), purchaseDate)
                        && plan.getTotalAmount().compareTo(totalAmount) == 0
                        && plan.getInstallmentCount() == installmentCount
                        && plan.getInstallmentAmount().compareTo(installmentAmount) == 0
                        && plan.getFirstInstallmentNumber() == firstInstallmentNumber
                        && Objects.equals(plan.getFirstInvoiceMonth(), firstInvoiceMonth)
                        && bigDecimalEquals(plan.getInterestRateMonthly(), interestRateMonthly)
                        && Objects.equals(plan.getCategoryId(), categoryId);
                if (!identical) {
                    return RowResult.error(
                            line, "parcelamento existente não pode ser alterado por importação; exclua e recrie");
                }
                return RowResult.updated(line);
            }

            if (installmentPlanRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                installmentPlanRepository.save(new InstallmentPlan(
                        id,
                        userId,
                        cardId,
                        description,
                        purchaseDate,
                        totalAmount,
                        installmentCount,
                        installmentAmount,
                        firstInstallmentNumber,
                        firstInvoiceMonth,
                        interestRateMonthly,
                        categoryId));
            }
            return RowResult.created(line);
        } catch (RowValidationException | java.time.format.DateTimeParseException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    private static boolean bigDecimalEquals(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        installmentPlanRepository.deleteAllByUserId(userId);
    }
}
