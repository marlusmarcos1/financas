package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.goal.domain.Goal;
import com.marlus.financas.goal.domain.GoalType;
import com.marlus.financas.goal.repository.GoalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GoalCsvHandler implements EntityCsvHandler {

    private final GoalRepository goalRepository;

    public GoalCsvHandler(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @Override
    public String fileName() {
        return "goals.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id",
                "name",
                "type",
                "target_amount",
                "target_date",
                "linked_account_id",
                "monthly_contribution_planned",
                "priority",
                "notes");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return goalRepository.findAllByUserIdOrderByPriorityAscNameAsc(userId).stream()
                .map(g -> List.of(
                        g.getId().toString(),
                        g.getName(),
                        g.getType().name(),
                        CsvFieldParser.required(g.getTargetAmount()),
                        CsvFieldParser.opt(g.getTargetDate()),
                        CsvFieldParser.opt(g.getLinkedAccountId()),
                        CsvFieldParser.required(g.getMonthlyContributionPlanned()),
                        String.valueOf(g.getPriority()),
                        CsvFieldParser.opt(g.getNotes())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String name = CsvFieldParser.requiredText(row, "name");
            GoalType type = CsvFieldParser.requiredEnum(row, "type", GoalType.class);
            BigDecimal targetAmount = CsvFieldParser.requiredDecimal(row, "target_amount");
            LocalDate targetDate = CsvFieldParser.optionalDate(row, "target_date");
            UUID linkedAccountId = CsvFieldParser.optionalUuid(row, "linked_account_id");
            BigDecimal monthlyContributionPlanned = CsvFieldParser.requiredDecimal(row, "monthly_contribution_planned");
            short priority = CsvFieldParser.requiredShort(row, "priority");
            String notes = CsvFieldParser.optionalText(row, "notes");

            var existing = goalRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                Goal goal = existing.get();
                if (apply) {
                    goal.setName(name);
                    goal.setType(type);
                    goal.setTargetAmount(targetAmount);
                    goal.setTargetDate(targetDate);
                    goal.setLinkedAccountId(linkedAccountId);
                    goal.setMonthlyContributionPlanned(monthlyContributionPlanned);
                    goal.setPriority(priority);
                    goal.setNotes(notes);
                    goalRepository.save(goal);
                }
                return RowResult.updated(line);
            }

            if (goalRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                goalRepository.save(new Goal(
                        id, userId, name, type, targetAmount, targetDate, linkedAccountId,
                        monthlyContributionPlanned, priority, notes));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        goalRepository.deleteAllByUserId(userId);
    }
}
