package com.marlus.financas.category.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.domain.Category;
import com.marlus.financas.category.repository.BudgetRepository;
import com.marlus.financas.category.repository.CategoryRepository;
import com.marlus.financas.category.web.CategoryRequest;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final CurrentUserProvider currentUserProvider;

    public CategoryService(
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository,
            CurrentUserProvider currentUserProvider) {
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAllByUserIdOrderByNameAsc(currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public Category findById(UUID id) {
        return categoryRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
    }

    public Category create(CategoryRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        Category parent = validateParent(request.parentId(), request.kind(), userId, null);
        Category category = new Category(
                UuidV7Generator.generate(),
                userId,
                request.name(),
                request.kind(),
                request.nature(),
                parent == null ? null : parent.getId(),
                request.icon(),
                request.color());
        return categoryRepository.save(category);
    }

    public Category update(UUID id, CategoryRequest request) {
        Category category = findById(id);
        Category parent = validateParent(request.parentId(), request.kind(), category.getUserId(), id);
        category.setName(request.name());
        category.setKind(request.kind());
        category.setNature(request.nature());
        category.setParentId(parent == null ? null : parent.getId());
        category.setIcon(request.icon());
        category.setColor(request.color());
        return category;
    }

    public void delete(UUID id) {
        UUID userId = currentUserProvider.currentUserId();
        Category category = findById(id);
        if (categoryRepository.existsByParentIdAndUserId(id, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Não é possível excluir: existem subcategorias vinculadas.");
        }
        if (budgetRepository.existsByCategoryIdAndUserId(id, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Não é possível excluir: existem orçamentos vinculados.");
        }
        categoryRepository.delete(category);
    }

    private Category validateParent(UUID parentId, com.marlus.financas.category.domain.CategoryKind kind, UUID userId, UUID selfId) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(selfId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma categoria não pode ser pai dela mesma.");
        }
        Category parent = categoryRepository
                .findByIdAndUserId(parentId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria pai não encontrada."));
        if (parent.getKind() != kind) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A categoria pai precisa ser do mesmo tipo (receita/despesa).");
        }
        return parent;
    }
}
