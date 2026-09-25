package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.goal.domain.GoalContribution;
import com.marlus.financas.goal.domain.GoalContributionSource;
import com.marlus.financas.goal.repository.GoalContributionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@link GoalContribution} é imutável (sem setters) — mesma regra do plano de parcelamento. */
@Component
public class GoalContributionCsvHandler implements EntityCsvHandler {

    private final GoalContributionRepository goalContributionRepository;

    public GoalContributionCsvHandler(GoalContributionRepository goalContributionRepository) {
        this.goalContributionRepository = goalContributionRepository;
    }

    @Override
    public String fileName() {
        return "goal_contributions.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "goal_id", "date", "amount", "source");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return goalContributionRepository.findAllByUserId(userId).stream()
                .map(c -> List.of(
                        c.getId().toString(),
                        c.getGoalId().toString(),
                        c.getDate().toString(),
                        CsvFieldParser.required(c.getAmount()),
                        c.getSource().name()))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            UUID goalId = CsvFieldParser.requiredUuid(row, "goal_id");
            LocalDate date = CsvFieldParser.requiredDate(row, "date");
            BigDecimal amount = CsvFieldParser.requiredDecimal(row, "amount");
            GoalContributionSource source = CsvFieldParser.requiredEnum(row, "source", GoalContributionSource.class);

            var existing = goalContributionRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                GoalContribution contribution = existing.get();
                boolean identical = Objects.equals(contribution.getGoalId(), goalId)
                        && Objects.equals(contribution.getDate(), date)
                        && contribution.getAmount().compareTo(amount) == 0
                        && contribution.getSource() == source;
                if (!identical) {
                    return RowResult.error(
                            line,
                            "aporte de meta existente não pode ser alterado por importação; exclua e recrie");
                }
                return RowResult.updated(line);
            }

            if (goalContributionRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                goalContributionRepository.save(new GoalContribution(id, userId, goalId, date, amount, source));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        goalContributionRepository.deleteAllByUserId(userId);
    }
}
