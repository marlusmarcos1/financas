package com.marlus.financas.category.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "category")
public class Category extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryNature nature;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(length = 40)
    private String icon;

    @Column(length = 7)
    private String color;

    protected Category() {
    }

    public Category(
            UUID id,
            UUID userId,
            String name,
            CategoryKind kind,
            CategoryNature nature,
            UUID parentId,
            String icon,
            String color) {
        super(userId);
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.nature = nature;
        this.parentId = parentId;
        this.icon = icon;
        this.color = color;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryKind getKind() {
        return kind;
    }

    public void setKind(CategoryKind kind) {
        this.kind = kind;
    }

    public CategoryNature getNature() {
        return nature;
    }

    public void setNature(CategoryNature nature) {
        this.nature = nature;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
