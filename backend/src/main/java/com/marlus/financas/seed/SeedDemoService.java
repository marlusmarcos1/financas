package com.marlus.financas.seed;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.domain.AccountPurpose;
import com.marlus.financas.account.domain.AccountType;
import com.marlus.financas.account.service.AccountService;
import com.marlus.financas.account.web.AccountRequest;
import com.marlus.financas.allocation.service.AllocationTargetService;
import com.marlus.financas.allocation.web.AllocationTargetRequest;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.domain.Category;
import com.marlus.financas.category.domain.CategoryKind;
import com.marlus.financas.category.domain.CategoryNature;
import com.marlus.financas.category.service.CategoryService;
import com.marlus.financas.category.web.CategoryRequest;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.service.CreditCardService;
import com.marlus.financas.creditcard.web.CreditCardRequest;
import com.marlus.financas.goal.domain.GoalType;
import com.marlus.financas.goal.service.GoalService;
import com.marlus.financas.goal.web.GoalRequest;
import com.marlus.financas.income.domain.IncomeRecurrence;
import com.marlus.financas.income.domain.IncomeSourceType;
import com.marlus.financas.income.service.IncomeSourceService;
import com.marlus.financas.income.web.IncomeSourceRequest;
import com.marlus.financas.installment.service.InstallmentPlanService;
import com.marlus.financas.installment.web.InstallmentPlanRequest;
import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import com.marlus.financas.recurring.domain.RecurringFrequency;
import com.marlus.financas.recurring.service.RecurringRuleService;
import com.marlus.financas.recurring.web.RecurringRuleRequest;
import com.marlus.financas.retirement.service.RetirementPlanService;
import com.marlus.financas.retirement.web.RetirementPlanRequest;
import com.marlus.financas.settings.domain.AppSetting;
import com.marlus.financas.settings.repository.AppSettingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados de exemplo (seção 10), idempotente: só semeia se o usuário atual ainda não tiver
 * (marcador {@code seed_demo_applied} em {@code app_setting}). Valores de dia de
 * fechamento/vencimento e algumas quantias marcadas como "a confirmar" na especificação usam
 * placeholders razoáveis, editáveis depois pela interface.
 */
@Service
@Transactional
public class SeedDemoService {

    private static final String MARKER_KEY = "seed_demo_applied";

    private final AppSettingRepository appSettingRepository;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final CreditCardService creditCardService;
    private final IncomeSourceService incomeSourceService;
    private final InstallmentPlanService installmentPlanService;
    private final RecurringRuleService recurringRuleService;
    private final RetirementPlanService retirementPlanService;
    private final AllocationTargetService allocationTargetService;
    private final GoalService goalService;
    private final CurrentUserProvider currentUserProvider;

    public SeedDemoService(
            AppSettingRepository appSettingRepository,
            CategoryService categoryService,
            AccountService accountService,
            CreditCardService creditCardService,
            IncomeSourceService incomeSourceService,
            InstallmentPlanService installmentPlanService,
            RecurringRuleService recurringRuleService,
            RetirementPlanService retirementPlanService,
            AllocationTargetService allocationTargetService,
            GoalService goalService,
            CurrentUserProvider currentUserProvider) {
        this.appSettingRepository = appSettingRepository;
        this.categoryService = categoryService;
        this.accountService = accountService;
        this.creditCardService = creditCardService;
        this.incomeSourceService = incomeSourceService;
        this.installmentPlanService = installmentPlanService;
        this.recurringRuleService = recurringRuleService;
        this.retirementPlanService = retirementPlanService;
        this.allocationTargetService = allocationTargetService;
        this.goalService = goalService;
        this.currentUserProvider = currentUserProvider;
    }

    public SeedDemoResponse seed() {
        UUID userId = currentUserProvider.currentUserId();
        boolean alreadyApplied = appSettingRepository.findByUserIdAndKey(userId, MARKER_KEY).isPresent();
        if (alreadyApplied) {
            return new SeedDemoResponse(false, "Dados de exemplo já foram carregados para este usuário.");
        }

        Map<String, UUID> categories = seedCategories();
        Account reserve = accountService.create(new AccountRequest(
                "Nubank – Caixinha (Minha reserva)", AccountType.SAVINGS_BOX, "Nubank", BigDecimal.ZERO,
                AccountPurpose.EMERGENCY_RESERVE));

        Map<String, CreditCard> cards = seedCards(reserve.getId());
        seedIncomeSources();
        seedInstallmentPlans(cards, categories);
        seedRecurringRules(cards, reserve.getId(), categories);
        seedRetirementPlan();
        seedGoals(reserve.getId());

        appSettingRepository.save(new AppSetting(UuidV7Generator.generate(), userId, MARKER_KEY, "true"));
        return new SeedDemoResponse(true, "Dados de exemplo carregados. Tudo pode ser editado ou excluído pela interface.");
    }

    private Map<String, UUID> seedCategories() {
        Map<String, UUID> ids = new HashMap<>();
        UUID receitas = categoryService.create(new CategoryRequest("Receitas", CategoryKind.INCOME, CategoryNature.FIXED, null, null, null)).getId();
        for (String name : new String[] {"Salário", "Bolsa", "13º Salário", "Extras"}) {
            ids.put(name, categoryService.create(
                    new CategoryRequest(name, CategoryKind.INCOME, CategoryNature.VARIABLE, receitas, null, null)).getId());
        }

        record ExpenseCategory(String name, CategoryNature nature) {
        }
        ExpenseCategory[] expenseCategories = {
            new ExpenseCategory("Moradia/Casa", CategoryNature.FIXED),
            new ExpenseCategory("Combustível", CategoryNature.VARIABLE),
            new ExpenseCategory("Transporte", CategoryNature.VARIABLE),
            new ExpenseCategory("Cartões", CategoryNature.FIXED),
            new ExpenseCategory("Assinaturas", CategoryNature.FIXED),
            new ExpenseCategory("Internet", CategoryNature.FIXED),
            new ExpenseCategory("Ajuda familiar", CategoryNature.FIXED),
            new ExpenseCategory("Dízimo", CategoryNature.FIXED),
            new ExpenseCategory("Aposentadoria/Investimentos", CategoryNature.FIXED),
            new ExpenseCategory("Lazer", CategoryNature.VARIABLE),
            new ExpenseCategory("Saúde", CategoryNature.VARIABLE),
            new ExpenseCategory("Compras pontuais", CategoryNature.VARIABLE),
            new ExpenseCategory("Extras", CategoryNature.VARIABLE),
        };
        for (ExpenseCategory category : expenseCategories) {
            ids.put(category.name(), categoryService.create(
                    new CategoryRequest(category.name(), CategoryKind.EXPENSE, category.nature(), null, null, null)).getId());
        }
        return ids;
    }

    private Map<String, CreditCard> seedCards(UUID defaultPaymentAccountId) {
        Map<String, CreditCard> cards = new HashMap<>();
        record CardSeed(String name, String issuer, short closingDay, short dueDay) {
        }
        CardSeed[] seeds = {
            new CardSeed("Hiper Mãe", "Hiper", (short) 5, (short) 12),
            new CardSeed("Carrefour", "Carrefour", (short) 10, (short) 17),
            new CardSeed("Itaú", "Itaú", (short) 15, (short) 22),
            new CardSeed("Nubank", "Nubank", (short) 20, (short) 27),
            new CardSeed("C6 Bank", "C6 Bank", (short) 25, (short) 3),
        };
        for (CardSeed seed : seeds) {
            CreditCard card = creditCardService.create(new CreditCardRequest(
                    seed.name(), seed.issuer(), new BigDecimal("2000.00"), seed.closingDay(), seed.dueDay(),
                    defaultPaymentAccountId, null));
            cards.put(seed.name(), card);
        }
        return cards;
    }

    private void seedIncomeSources() {
        incomeSourceService.create(new IncomeSourceRequest(
                "Salário", IncomeSourceType.SALARY, IncomeRecurrence.MONTHLY, new BigDecimal("5031.74"),
                (short) 5, LocalDate.now().withDayOfMonth(1), null, null, true, true));
        incomeSourceService.create(new IncomeSourceRequest(
                "Bolsa", IncomeSourceType.SCHOLARSHIP, IncomeRecurrence.TEMPORARY, new BigDecimal("3800.00"),
                (short) 5, LocalDate.now().withDayOfMonth(1), null, (short) 15, true, false));
        incomeSourceService.create(new IncomeSourceRequest(
                "13º Salário", IncomeSourceType.THIRTEENTH, IncomeRecurrence.SPORADIC, BigDecimal.ZERO,
                null, null, null, null, true, false));
        incomeSourceService.create(new IncomeSourceRequest(
                "Extras", IncomeSourceType.EXTRA, IncomeRecurrence.SPORADIC, BigDecimal.ZERO,
                null, null, null, null, true, false));
    }

    private void seedInstallmentPlans(Map<String, CreditCard> cards, Map<String, UUID> categories) {
        installmentPlanService.create(new InstallmentPlanRequest(
                cards.get("Carrefour").getId(), "Colchão", LocalDate.of(2026, 7, 15), new BigDecimal("4140.00"),
                (short) 12, (short) 1, YearMonth.of(2026, 8), BigDecimal.ZERO, categories.get("Moradia/Casa")));
        installmentPlanService.create(new InstallmentPlanRequest(
                cards.get("Carrefour").getId(), "Cama", LocalDate.now(), new BigDecimal("2092.20"),
                (short) 10, (short) 1, YearMonth.now(), BigDecimal.ZERO, categories.get("Moradia/Casa")));
        installmentPlanService.create(new InstallmentPlanRequest(
                cards.get("Itaú").getId(), "Celular", LocalDate.of(2026, 3, 1), new BigDecimal("3250.00"),
                (short) 10, (short) 1, YearMonth.of(2026, 3), BigDecimal.ZERO, categories.get("Compras pontuais")));
        installmentPlanService.create(new InstallmentPlanRequest(
                cards.get("Itaú").getId(), "Outro parcelamento (valor a confirmar)", LocalDate.of(2026, 4, 1),
                new BigDecimal("2424.00"), (short) 12, (short) 1, YearMonth.of(2026, 4), BigDecimal.ZERO,
                categories.get("Compras pontuais")));
        installmentPlanService.create(new InstallmentPlanRequest(
                cards.get("Itaú").getId(), "IPVA do carro", LocalDate.now(), new BigDecimal("653.91"),
                (short) 3, (short) 1, YearMonth.now(), BigDecimal.ZERO, categories.get("Transporte")));
    }

    private void seedRecurringRules(Map<String, CreditCard> cards, UUID accountId, Map<String, UUID> categories) {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        recurringRuleService.create(new RecurringRuleRequest(
                "Combustível", new BigDecimal("950.00"), true, RecurringFrequency.MONTHLY, (short) 10, start, null,
                categories.get("Combustível"), cards.get("Carrefour").getId(), null, true));
        recurringRuleService.create(new RecurringRuleRequest(
                "Meli+", new BigDecimal("79.00"), false, RecurringFrequency.MONTHLY, (short) 15, start, null,
                categories.get("Assinaturas"), cards.get("C6 Bank").getId(), null, true));
        recurringRuleService.create(new RecurringRuleRequest(
                "Claude", new BigDecimal("120.00"), false, RecurringFrequency.MONTHLY, (short) 15, start, null,
                categories.get("Assinaturas"), cards.get("C6 Bank").getId(), null, true));
        recurringRuleService.create(new RecurringRuleRequest(
                "Internet (Net/Claro)", new BigDecimal("120.00"), true, RecurringFrequency.MONTHLY, (short) 8, start,
                null, categories.get("Internet"), null, accountId, true));
        recurringRuleService.create(new RecurringRuleRequest(
                "Crédito de celular (tia)", new BigDecimal("140.00"), false, RecurringFrequency.MONTHLY, (short) 5,
                start, null, categories.get("Ajuda familiar"), null, accountId, true));
        recurringRuleService.create(new RecurringRuleRequest(
                "Banese", new BigDecimal("178.90"), false, RecurringFrequency.MONTHLY, (short) 12, start, null,
                categories.get("Cartões"), null, accountId, true));
    }

    private void seedRetirementPlan() {
        retirementPlanService.upsert(new RetirementPlanRequest(
                new BigDecimal("500.00"), BigDecimal.ZERO, LocalDate.now(), (short) 30, new BigDecimal("8.00"),
                new BigDecimal("4.50"), BigDecimal.ZERO));
        allocationTargetService.upsert(
                new AllocationTargetRequest(InvestmentPurpose.RETIREMENT, InvestmentAssetClass.FII, new BigDecimal("20.00")));
        allocationTargetService.upsert(new AllocationTargetRequest(
                InvestmentPurpose.RETIREMENT, InvestmentAssetClass.OTHER, new BigDecimal("80.00")));
    }

    private void seedGoals(UUID reserveAccountId) {
        goalService.create(new GoalRequest(
                "Reserva de emergência", GoalType.EMERGENCY, new BigDecimal("18000.00"), null, reserveAccountId,
                BigDecimal.ZERO, (short) 1, "6 meses de gastos essenciais."));
        goalService.create(new GoalRequest(
                "Casa (entrada)", GoalType.HOUSE, new BigDecimal("50000.00"), LocalDate.of(2027, 12, 12), null,
                BigDecimal.ZERO, (short) 2, "Valor-alvo a confirmar."));
        goalService.create(new GoalRequest(
                "Carro", GoalType.CAR, new BigDecimal("30000.00"), null, null, BigDecimal.ZERO, (short) 3, null));
    }
}
