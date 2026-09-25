package com.marlus.financas.dataio.handler;

import com.marlus.financas.category.domain.Category;
import com.marlus.financas.category.domain.CategoryKind;
import com.marlus.financas.category.domain.CategoryNature;
import com.marlus.financas.category.repository.CategoryRepository;
import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CategoryCsvHandler implements EntityCsvHandler {

    private final CategoryRepository categoryRepository;

    public CategoryCsvHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public String fileName() {
        return "categories.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "name", "kind", "nature", "parent_id", "icon", "color");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(c -> List.of(
                        c.getId().toString(),
                        c.getName(),
                        c.getKind().name(),
                        c.getNature().name(),
                        CsvFieldParser.opt(c.getParentId()),
                        CsvFieldParser.opt(c.getIcon()),
                        CsvFieldParser.opt(c.getColor())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String name = CsvFieldParser.requiredText(row, "name");
            CategoryKind kind = CsvFieldParser.requiredEnum(row, "kind", CategoryKind.class);
            CategoryNature nature = CsvFieldParser.requiredEnum(row, "nature", CategoryNature.class);
            UUID parentId = CsvFieldParser.optionalUuid(row, "parent_id");
            String icon = CsvFieldParser.optionalText(row, "icon");
            String color = CsvFieldParser.optionalText(row, "color");

            var existing = categoryRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                Category category = existing.get();
                if (apply) {
                    category.setName(name);
                    category.setKind(kind);
                    category.setNature(nature);
                    category.setParentId(parentId);
                    category.setIcon(icon);
                    category.setColor(color);
                    categoryRepository.save(category);
                }
                return RowResult.updated(line);
            }

            if (categoryRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                categoryRepository.save(new Category(id, userId, name, kind, nature, parentId, icon, color));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        categoryRepository.deleteAllByUserId(userId);
    }
}
