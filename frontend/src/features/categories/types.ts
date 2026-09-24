export type CategoryKind = "INCOME" | "EXPENSE";
export type CategoryNature = "FIXED" | "VARIABLE";

export interface Category {
  id: string;
  name: string;
  kind: CategoryKind;
  nature: CategoryNature;
  parentId: string | null;
  icon: string | null;
  color: string | null;
}

export interface CategoryInput {
  name: string;
  kind: CategoryKind;
  nature: CategoryNature;
  parentId: string | null;
  icon: string;
  color: string;
}

export interface Budget {
  id: string;
  categoryId: string;
  month: string | null;
  limitAmount: number;
}

export interface BudgetInput {
  month: string | null;
  limitAmount: number;
}

export const categoryKindLabels: Record<CategoryKind, string> = {
  INCOME: "Receita",
  EXPENSE: "Despesa",
};

export const categoryNatureLabels: Record<CategoryNature, string> = {
  FIXED: "Fixa",
  VARIABLE: "Variável",
};
