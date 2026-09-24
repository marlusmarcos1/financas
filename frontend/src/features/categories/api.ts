import { api } from "@/lib/api";
import type { Budget, BudgetInput, Category, CategoryInput } from "./types";

export function fetchCategories(): Promise<Category[]> {
  return api.get<Category[]>("/categories");
}

export function createCategory(input: CategoryInput): Promise<Category> {
  return api.post<Category>("/categories", input);
}

export function updateCategory(id: string, input: CategoryInput): Promise<Category> {
  return api.put<Category>(`/categories/${id}`, input);
}

export function deleteCategory(id: string): Promise<void> {
  return api.del<void>(`/categories/${id}`);
}

export function fetchBudgets(categoryId: string): Promise<Budget[]> {
  return api.get<Budget[]>(`/categories/${categoryId}/budgets`);
}

export function createBudget(categoryId: string, input: BudgetInput): Promise<Budget> {
  return api.post<Budget>(`/categories/${categoryId}/budgets`, input);
}

export function deleteBudget(categoryId: string, id: string): Promise<void> {
  return api.del<void>(`/categories/${categoryId}/budgets/${id}`);
}
