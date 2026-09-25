package com.marlus.financas.dashboard.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.service.BudgetSummaryService;
import com.marlus.financas.category.web.CategoryBudgetStatusResponse;
import com.marlus.financas.commitment.service.CommitmentService;
import com.marlus.financas.commitment.web.MonthCommitmentResponse;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.dashboard.web.AlertResponse;
import com.marlus.financas.income.domain.IncomeRecurrence;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import com.marlus.financas.invoice.domain.Invoice;
import com.marlus.financas.invoice.domain.InvoiceStatus;
import com.marlus.financas.invoice.repository.InvoiceRepository;
import com.marlus.financas.settings.service.SettingsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alertas do dashboard (seção 8 / 11): teto de categoria estourando, fatura fechando/vencendo,
 * bolsa perto do fim, sobra normalizada negativa e comprometimento com parcelas acima do limite.
 */
@Service
@Transactional(readOnly = true)
public class AlertsService {

    private static final int DUE_SOON_DAYS = 3;
    private static final int SCHOLARSHIP_WARNING_MONTHS = 2;

    private final CreditCardRepository creditCardRepository;
    private final InvoiceRepository invoiceRepository;
    private final BudgetSummaryService budgetSummaryService;
    private final IncomeSourceRepository incomeSourceRepository;
    private final DashboardService dashboardService;
    private final CommitmentService commitmentService;
    private final SettingsService settingsService;
    private final CurrentUserProvider currentUserProvider;

    public AlertsService(
            CreditCardRepository creditCardRepository,
            InvoiceRepository invoiceRepository,
            BudgetSummaryService budgetSummaryService,
            IncomeSourceRepository incomeSourceRepository,
            DashboardService dashboardService,
            CommitmentService commitmentService,
            SettingsService settingsService,
            CurrentUserProvider currentUserProvider) {
        this.creditCardRepository = creditCardRepository;
        this.invoiceRepository = invoiceRepository;
        this.budgetSummaryService = budgetSummaryService;
        this.incomeSourceRepository = incomeSourceRepository;
        this.dashboardService = dashboardService;
        this.commitmentService = commitmentService;
        this.settingsService = settingsService;
        this.currentUserProvider = currentUserProvider;
    }

    public List<AlertResponse> list() {
        UUID userId = currentUserProvider.currentUserId();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        List<AlertResponse> alerts = new ArrayList<>();

        Map<UUID, String> cardNames = creditCardRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId).stream()
                .collect(Collectors.toMap(CreditCard::getId, CreditCard::getName));

        for (Invoice invoice : invoiceRepository.findAllByUserId(userId)) {
            String cardName = cardNames.getOrDefault(invoice.getCardId(), "cartão");
            if (invoice.getStatus() == InvoiceStatus.OPEN) {
                long daysToClose = ChronoUnit.DAYS.between(today, invoice.getClosingDate());
                if (daysToClose >= 0 && daysToClose <= DUE_SOON_DAYS) {
                    alerts.add(AlertResponse.warning(
                            "Fatura do " + cardName + " fecha em " + daysToClose + " dia(s)."));
                }
            }
            if (invoice.getStatus() != InvoiceStatus.PAID) {
                long daysToDue = ChronoUnit.DAYS.between(today, invoice.getDueDate());
                if (daysToDue >= 0 && daysToDue <= DUE_SOON_DAYS) {
                    alerts.add(AlertResponse.warning(
                            "Fatura do " + cardName + " vence em " + daysToDue + " dia(s)."));
                } else if (daysToDue < 0) {
                    alerts.add(AlertResponse.danger("Fatura do " + cardName + " está vencida."));
                }
            }
        }

        for (CategoryBudgetStatusResponse status : budgetSummaryService.summarize(currentMonth)) {
            if ("RED".equals(status.status())) {
                alerts.add(AlertResponse.danger(
                        status.categoryName() + " já passou do teto do mês (" + status.percent() + "%)."));
            } else if ("YELLOW".equals(status.status())) {
                alerts.add(AlertResponse.warning(
                        status.categoryName() + " está em " + status.percent() + "% do teto do mês."));
            }
        }

        for (IncomeSource source : incomeSourceRepository.findAllByUserIdOrderByNameAsc(userId)) {
            if (source.getRecurrence() != IncomeRecurrence.TEMPORARY
                    || source.getStartDate() == null
                    || source.getExpectedMonths() == null) {
                continue;
            }
            long elapsed = ChronoUnit.MONTHS.between(YearMonth.from(source.getStartDate()), currentMonth);
            long remaining = source.getExpectedMonths() - elapsed;
            if (remaining >= 0 && remaining <= SCHOLARSHIP_WARNING_MONTHS) {
                alerts.add(AlertResponse.warning(
                        source.getName() + " termina em " + remaining + " mês(es)."));
            }
        }

        if (dashboardService.summarize(currentMonth).surplus().signum() < 0) {
            alerts.add(AlertResponse.danger("A sobra do mês está negativa."));
        }

        List<MonthCommitmentResponse> commitment = commitmentService.summarize(1);
        BigDecimal baseIncome = dashboardService.summarize(currentMonth).baseIncomeExpected();
        if (!commitment.isEmpty() && baseIncome.signum() > 0) {
            BigDecimal limitPercent = settingsService.getSettings().installmentLimitPercent();
            BigDecimal committedPercent = commitment.get(0)
                    .installmentsTotal()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(baseIncome, 2, RoundingMode.HALF_EVEN);
            if (committedPercent.compareTo(limitPercent) > 0) {
                alerts.add(AlertResponse.danger(
                        "Comprometimento com parcelas em " + committedPercent + "% (limite " + limitPercent + "%)."));
            }
        }

        return alerts;
    }
}
