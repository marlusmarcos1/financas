package com.marlus.financas.recurring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Materializa as recorrências ativas de todos os usuários diariamente (e ao abrir cada mês),
 * garantindo que os lançamentos PLANNED dos próximos 12 meses existam. Ver seção 6 (recurring_rule).
 */
@Component
public class RecurringRuleMaterializationJob {

    private static final Logger log = LoggerFactory.getLogger(RecurringRuleMaterializationJob.class);

    private final RecurringRuleService recurringRuleService;

    public RecurringRuleMaterializationJob(RecurringRuleService recurringRuleService) {
        this.recurringRuleService = recurringRuleService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void materializeDaily() {
        log.info("Materializando recorrências ativas...");
        recurringRuleService.materializeAllActive();
    }
}
