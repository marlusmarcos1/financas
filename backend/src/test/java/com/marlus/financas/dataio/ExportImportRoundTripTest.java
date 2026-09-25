package com.marlus.financas.dataio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.domain.AccountPurpose;
import com.marlus.financas.account.domain.AccountType;
import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.allocation.domain.AllocationTarget;
import com.marlus.financas.allocation.repository.AllocationTargetRepository;
import com.marlus.financas.category.domain.Budget;
import com.marlus.financas.category.domain.Category;
import com.marlus.financas.category.domain.CategoryKind;
import com.marlus.financas.category.domain.CategoryNature;
import com.marlus.financas.category.repository.BudgetRepository;
import com.marlus.financas.category.repository.CategoryRepository;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.goal.domain.Goal;
import com.marlus.financas.goal.domain.GoalContribution;
import com.marlus.financas.goal.domain.GoalContributionSource;
import com.marlus.financas.goal.domain.GoalType;
import com.marlus.financas.goal.repository.GoalContributionRepository;
import com.marlus.financas.goal.repository.GoalRepository;
import com.marlus.financas.income.domain.IncomeEntry;
import com.marlus.financas.income.domain.IncomeEntryStatus;
import com.marlus.financas.income.domain.IncomeRecurrence;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.domain.IncomeSourceType;
import com.marlus.financas.income.repository.IncomeEntryRepository;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import com.marlus.financas.installment.domain.InstallmentPlan;
import com.marlus.financas.installment.repository.InstallmentPlanRepository;
import com.marlus.financas.investment.domain.InvestmentAsset;
import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import com.marlus.financas.investment.domain.InvestmentTransaction;
import com.marlus.financas.investment.domain.InvestmentTransactionType;
import com.marlus.financas.investment.repository.InvestmentAssetRepository;
import com.marlus.financas.investment.repository.InvestmentTransactionRepository;
import com.marlus.financas.invoice.domain.Invoice;
import com.marlus.financas.invoice.repository.InvoiceRepository;
import com.marlus.financas.recurring.domain.RecurringFrequency;
import com.marlus.financas.recurring.domain.RecurringRule;
import com.marlus.financas.recurring.repository.RecurringRuleRepository;
import com.marlus.financas.retirement.domain.RetirementPlan;
import com.marlus.financas.retirement.repository.RetirementPlanRepository;
import com.marlus.financas.settings.domain.AppSetting;
import com.marlus.financas.settings.repository.AppSettingRepository;
import com.marlus.financas.tithe.domain.TitheLedger;
import com.marlus.financas.tithe.repository.TitheLedgerRepository;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.user.AppUserRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

class ExportImportRoundTripTest extends AbstractIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private IncomeSourceRepository incomeSourceRepository;

    @Autowired
    private IncomeEntryRepository incomeEntryRepository;

    @Autowired
    private RecurringRuleRepository recurringRuleRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TitheLedgerRepository titheLedgerRepository;

    @Autowired
    private InvestmentAssetRepository investmentAssetRepository;

    @Autowired
    private InvestmentTransactionRepository investmentTransactionRepository;

    @Autowired
    private AllocationTargetRepository allocationTargetRepository;

    @Autowired
    private RetirementPlanRepository retirementPlanRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @Test
    void exportThenImportIntoFreshDbProducesIdenticalExport() throws Exception {
        MockHttpSession session = createUserAndLogin("dataio_export_a", "senhaSegura123");
        UUID userId = appUserRepository.findByUsername("dataio_export_a").orElseThrow().getId();
        seedFullDataset(userId);

        byte[] zipA = exportZip(session);
        Map<String, byte[]> filesA = unzip(zipA);
        assertThat(filesA).containsKey("manifest.json");
        assertThat(filesA.get("accounts.csv")).isNotEmpty();

        // Simula "banco zerado" usando o próprio modo REPLACE: apaga tudo e reimporta o mesmo zip
        // em uma única transação (exercita também o caminho de "Substituir tudo" com confirmação).
        MockMultipartFile upload = new MockMultipartFile("file", "export.zip", "application/zip", zipA);
        var applyResult = mockMvc.perform(multipart("/api/v1/data/import/apply")
                        .file(upload)
                        .param("mode", "REPLACE")
                        .param("confirmation", "SUBSTITUIR")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> summary = objectMapper.readValue(
                applyResult.getResponse().getContentAsByteArray(), Map.class);
        assertThat(summary.get("totalErrors")).isEqualTo(0);
        assertThat((Integer) summary.get("totalCreated")).isGreaterThan(0);

        byte[] zipB = exportZip(session);
        Map<String, byte[]> filesB = unzip(zipB);

        for (String fileName : filesA.keySet()) {
            if (fileName.equals("manifest.json")) {
                continue;
            }
            assertThat(filesB).containsKey(fileName);
            assertThat(new String(filesB.get(fileName), StandardCharsets.UTF_8))
                    .as("conteúdo de %s deve ser idêntico após export -> import -> export", fileName)
                    .isEqualTo(new String(filesA.get(fileName), StandardCharsets.UTF_8));
        }

        // Reimportar o mesmo arquivo (dados já presentes) deve ser idempotente: tudo "updated", zero erros.
        var secondApply = mockMvc.perform(multipart("/api/v1/data/import/apply")
                        .file(upload)
                        .param("mode", "MERGE")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> secondSummary =
                objectMapper.readValue(secondApply.getResponse().getContentAsByteArray(), Map.class);
        assertThat(secondSummary.get("totalErrors")).isEqualTo(0);
        assertThat(secondSummary.get("totalCreated")).isEqualTo(0);
        assertThat((Integer) secondSummary.get("totalUpdated")).isGreaterThan(0);
    }

    @Test
    void replaceModeWithoutConfirmationIsRejected() throws Exception {
        MockHttpSession session = createUserAndLogin("dataio_replace_noconfirm", "senhaSegura123");
        UUID userId = appUserRepository.findByUsername("dataio_replace_noconfirm").orElseThrow().getId();
        seedFullDataset(userId);
        byte[] zip = exportZip(session);

        MockMultipartFile upload = new MockMultipartFile("file", "export.zip", "application/zip", zip);
        mockMvc.perform(multipart("/api/v1/data/import/apply")
                        .file(upload)
                        .param("mode", "REPLACE")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tamperedChecksumIsRejected() throws Exception {
        MockHttpSession session = createUserAndLogin("dataio_tamper", "senhaSegura123");
        UUID userId = appUserRepository.findByUsername("dataio_tamper").orElseThrow().getId();
        seedFullDataset(userId);
        byte[] zip = exportZip(session);

        byte[] tampered = tamperAccountsCsv(zip);
        MockMultipartFile upload = new MockMultipartFile("file", "export.zip", "application/zip", tampered);
        mockMvc.perform(multipart("/api/v1/data/import/dry-run").file(upload).session(session).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    private byte[] exportZip(MockHttpSession session) throws Exception {
        return mockMvc.perform(get("/api/v1/data/export").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private static Map<String, byte[]> unzip(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), zip.readAllBytes());
                zip.closeEntry();
            }
        }
        return entries;
    }

    private static byte[] tamperAccountsCsv(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = unzip(zipBytes);
        byte[] original = entries.get("accounts.csv");
        String tampered = new String(original, StandardCharsets.UTF_8) + "\r\nlinha;adulterada\r\n";
        entries.put("accounts.csv", tampered.getBytes(StandardCharsets.UTF_8));

        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            for (var e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return buffer.toByteArray();
    }

    private void seedFullDataset(UUID userId) {
        UUID accountId = UuidV7Generator.generate();
        accountRepository.save(new Account(
                accountId, userId, "Conta Corrente ç;\"", AccountType.CHECKING, "Banco X", new BigDecimal("100.00"),
                AccountPurpose.DAILY));

        UUID cardId = UuidV7Generator.generate();
        creditCardRepository.save(new CreditCard(
                cardId, userId, "Cartão Principal", "Bandeira Y", new BigDecimal("5000.00"), (short) 20, (short) 27,
                accountId, "#FF0000"));

        UUID categoryExpenseId = UuidV7Generator.generate();
        categoryRepository.save(new Category(
                categoryExpenseId, userId, "Mercado", CategoryKind.EXPENSE, CategoryNature.VARIABLE, null, "cart",
                "#00FF00"));
        UUID categoryIncomeId = UuidV7Generator.generate();
        categoryRepository.save(new Category(
                categoryIncomeId, userId, "Salário", CategoryKind.INCOME, CategoryNature.FIXED, null, "money",
                "#0000FF"));

        budgetRepository.save(
                new Budget(UuidV7Generator.generate(), userId, categoryExpenseId, "2026-01", new BigDecimal("600.00")));

        UUID incomeSourceId = UuidV7Generator.generate();
        incomeSourceRepository.save(new IncomeSource(
                incomeSourceId, userId, "Salário CLT", IncomeSourceType.SALARY, IncomeRecurrence.MONTHLY,
                new BigDecimal("3000.00"), (short) 5, LocalDate.of(2025, 1, 1), null, null, true, true));

        incomeEntryRepository.save(new IncomeEntry(
                UuidV7Generator.generate(), userId, incomeSourceId, accountId, "2026-01",
                LocalDate.of(2026, 1, 5), new BigDecimal("3000.00"), IncomeEntryStatus.RECEIVED));

        RecurringRule rule = new RecurringRule(
                UuidV7Generator.generate(), userId, "Internet", new BigDecimal("120.00"), false,
                RecurringFrequency.MONTHLY, (short) 10, LocalDate.of(2025, 1, 1), null, categoryExpenseId, cardId, null);
        recurringRuleRepository.save(rule);

        InstallmentPlan plan = new InstallmentPlan(
                UuidV7Generator.generate(), userId, cardId, "Notebook", LocalDate.of(2026, 1, 10),
                new BigDecimal("3000.00"), (short) 10, new BigDecimal("300.00"), (short) 1,
                YearMonth.of(2026, 2), new BigDecimal("0.00"), categoryExpenseId);
        installmentPlanRepository.save(plan);

        Invoice invoice = new Invoice(
                UuidV7Generator.generate(), userId, cardId, YearMonth.of(2026, 2), LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 27));
        invoice.registerPayment(new BigDecimal("150.00"), LocalDate.of(2026, 2, 26), accountId);
        invoiceRepository.save(invoice);

        transactionRepository.save(new Transaction(
                UuidV7Generator.generate(), userId, TransactionKind.EXPENSE, "Compra no mercado; com \"aspas\"",
                new BigDecimal("89.90"), LocalDate.of(2026, 1, 15), categoryExpenseId, accountId, null,
                TransactionStatus.PAID, "observação com ç e ã"));

        TitheLedger tithe = new TitheLedger(UuidV7Generator.generate(), userId, "2026-01", new BigDecimal("10.00"));
        tithe.recalculateBase(new BigDecimal("3000.00"), new BigDecimal("10.00"));
        tithe.registerPayment(new BigDecimal("300.00"), LocalDate.of(2026, 1, 10));
        titheLedgerRepository.save(tithe);

        UUID assetId = UuidV7Generator.generate();
        investmentAssetRepository.save(new InvestmentAsset(
                assetId, userId, "MXRF11", "Maxi Renda FII", InvestmentAssetClass.FII, "Papel", null, null,
                InvestmentPurpose.GENERAL));

        investmentTransactionRepository.save(new InvestmentTransaction(
                UuidV7Generator.generate(), userId, assetId, InvestmentTransactionType.BUY,
                LocalDate.of(2026, 1, 5), new BigDecimal("100"), new BigDecimal("10.00"), new BigDecimal("1.50"),
                new BigDecimal("1001.50"), accountId));

        allocationTargetRepository.save(new AllocationTarget(
                UuidV7Generator.generate(), userId, InvestmentPurpose.GENERAL, InvestmentAssetClass.FII,
                new BigDecimal("30.00")));

        retirementPlanRepository.save(new RetirementPlan(
                UuidV7Generator.generate(), userId, new BigDecimal("500.00"), new BigDecimal("5.00"),
                LocalDate.of(2026, 1, 1), (short) 30, new BigDecimal("8.00"), new BigDecimal("4.00"),
                new BigDecimal("0.00")));

        UUID goalId = UuidV7Generator.generate();
        goalRepository.save(new Goal(
                goalId, userId, "Viagem", GoalType.OTHER, new BigDecimal("5000.00"), LocalDate.of(2027, 1, 1),
                accountId, new BigDecimal("200.00"), (short) 1, "notas da meta"));

        goalContributionRepository.save(new GoalContribution(
                UuidV7Generator.generate(), userId, goalId, LocalDate.of(2026, 1, 20), new BigDecimal("200.00"),
                GoalContributionSource.MANUAL));

        appSettingRepository.save(new AppSetting(UuidV7Generator.generate(), userId, "tithe_percent", "10"));
    }
}
