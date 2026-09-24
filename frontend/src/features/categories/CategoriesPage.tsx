import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { ApiError } from "@/lib/api";
import {
  createBudget,
  createCategory,
  deleteBudget,
  deleteCategory,
  fetchBudgets,
  fetchCategories,
  updateCategory,
} from "./api";
import { categoryKindLabels, categoryNatureLabels, type Category } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const categorySchema = z.object({
  name: z.string().min(1, "Informe o nome da categoria."),
  kind: z.enum(["INCOME", "EXPENSE"]),
  nature: z.enum(["FIXED", "VARIABLE"]),
  parentId: z.string(),
  icon: z.string(),
  color: z.string(),
});

type CategoryFormData = z.infer<typeof categorySchema>;

export function CategoriesPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [budgetCategory, setBudgetCategory] = useState<Category | null>(null);

  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: fetchCategories });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["categories"] });

  const toInput = (data: CategoryFormData) => ({
    ...data,
    parentId: data.parentId || null,
  });

  const createMutation = useMutation({
    mutationFn: (data: CategoryFormData) => createCategory(toInput(data)),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Categoria criada.");
      setIsCreating(false);
    },
    onError: (error) =>
      toast.showError(error instanceof ApiError ? error.message : "Erro ao criar categoria."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CategoryFormData }) => updateCategory(id, toInput(data)),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Categoria atualizada.");
      setEditingCategory(null);
    },
    onError: (error) =>
      toast.showError(error instanceof ApiError ? error.message : "Erro ao atualizar categoria."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCategory,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Categoria excluída.");
    },
    onError: (error) =>
      toast.showError(error instanceof ApiError ? error.message : "Erro ao excluir categoria."),
  });

  const categories = categoriesQuery.data ?? [];
  const categoryNameById = new Map(categories.map((category) => [category.id, category.name]));

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Categorias</h1>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
        >
          Nova categoria
        </button>
      </div>

      {categoriesQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
      {categoriesQuery.isError && <p className="text-sm text-red-600">Erro ao carregar categorias.</p>}
      {categories.length === 0 && !categoriesQuery.isLoading && (
        <p className="text-sm text-slate-500">Nenhuma categoria cadastrada ainda.</p>
      )}

      {categories.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Nome</th>
                <th className="px-4 py-2">Tipo</th>
                <th className="px-4 py-2">Natureza</th>
                <th className="px-4 py-2">Categoria pai</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {categories.map((category) => (
                <tr key={category.id}>
                  <td className="px-4 py-2 font-medium">{category.name}</td>
                  <td className="px-4 py-2">{categoryKindLabels[category.kind]}</td>
                  <td className="px-4 py-2">{categoryNatureLabels[category.nature]}</td>
                  <td className="px-4 py-2">
                    {category.parentId ? (categoryNameById.get(category.parentId) ?? "—") : "—"}
                  </td>
                  <td className="px-4 py-2 text-right">
                    <div className="flex justify-end gap-2">
                      {category.kind === "EXPENSE" && (
                        <button
                          type="button"
                          onClick={() => setBudgetCategory(category)}
                          className="text-slate-600 hover:underline dark:text-slate-300"
                        >
                          Orçamento
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => setEditingCategory(category)}
                        className="text-slate-600 hover:underline dark:text-slate-300"
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (window.confirm(`Excluir a categoria "${category.name}"?`)) {
                            deleteMutation.mutate(category.id);
                          }
                        }}
                        className="text-red-600 hover:underline"
                      >
                        Excluir
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isCreating && (
        <CategoryFormModal
          title="Nova categoria"
          categories={categories}
          onClose={() => setIsCreating(false)}
          onSubmit={(data) => createMutation.mutate(data)}
        />
      )}

      {editingCategory && (
        <CategoryFormModal
          title="Editar categoria"
          initialValues={editingCategory}
          categories={categories.filter((category) => category.id !== editingCategory.id)}
          onClose={() => setEditingCategory(null)}
          onSubmit={(data) => updateMutation.mutate({ id: editingCategory.id, data })}
        />
      )}

      {budgetCategory && (
        <BudgetModal category={budgetCategory} onClose={() => setBudgetCategory(null)} />
      )}
    </div>
  );
}

function CategoryFormModal({
  title,
  initialValues,
  categories,
  onClose,
  onSubmit,
}: {
  title: string;
  initialValues?: Category;
  categories: Category[];
  onClose: () => void;
  onSubmit: (data: CategoryFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CategoryFormData>({
    resolver: zodResolver(categorySchema),
    defaultValues: initialValues
      ? {
          name: initialValues.name,
          kind: initialValues.kind,
          nature: initialValues.nature,
          parentId: initialValues.parentId ?? "",
          icon: initialValues.icon ?? "",
          color: initialValues.color ?? "",
        }
      : { name: "", kind: "EXPENSE", nature: "VARIABLE", parentId: "", icon: "", color: "" },
  });

  const selectedKind = watch("kind");
  const possibleParents = categories.filter((category) => category.kind === selectedKind);

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Nome" htmlFor="cat-name" error={errors.name?.message}>
          <input id="cat-name" className={inputClass} {...register("name")} />
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Tipo" htmlFor="cat-kind" error={errors.kind?.message}>
            <select id="cat-kind" className={inputClass} {...register("kind")}>
              {Object.entries(categoryKindLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Natureza" htmlFor="cat-nature" error={errors.nature?.message}>
            <select id="cat-nature" className={inputClass} {...register("nature")}>
              {Object.entries(categoryNatureLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </FormField>
        </div>

        <FormField label="Categoria pai (opcional)" htmlFor="cat-parent" error={errors.parentId?.message}>
          <select id="cat-parent" className={inputClass} {...register("parentId")}>
            <option value="">Nenhuma</option>
            {possibleParents.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Cor (opcional)" htmlFor="cat-color" error={errors.color?.message}>
          <input id="cat-color" type="color" className="h-10 w-16" {...register("color")} />
        </FormField>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
        >
          Salvar
        </button>
      </form>
    </Modal>
  );
}

const budgetSchema = z.object({
  month: z
    .string()
    .regex(/^(\d{4}-(0[1-9]|1[0-2]))?$/, "Use o formato AAAA-MM.")
    .optional()
    .or(z.literal("")),
  limitAmount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "Não pode ser negativo."),
});

type BudgetFormData = z.infer<typeof budgetSchema>;

function BudgetModal({ category, onClose }: { category: Category; onClose: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const budgetsQuery = useQuery({
    queryKey: ["budgets", category.id],
    queryFn: () => fetchBudgets(category.id),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<BudgetFormData>({
    resolver: zodResolver(budgetSchema),
    defaultValues: { month: "", limitAmount: 0 },
  });

  const createMutation = useMutation({
    mutationFn: (data: BudgetFormData) =>
      createBudget(category.id, { month: data.month || null, limitAmount: data.limitAmount }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["budgets", category.id] });
      toast.showSuccess("Teto de orçamento adicionado.");
      reset({ month: "", limitAmount: 0 });
    },
    onError: (error) =>
      toast.showError(error instanceof ApiError ? error.message : "Erro ao criar teto de orçamento."),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteBudget(category.id, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["budgets", category.id] });
      toast.showSuccess("Teto de orçamento removido.");
    },
    onError: () => toast.showError("Erro ao remover teto de orçamento."),
  });

  return (
    <Modal title={`Orçamento — ${category.name}`} onClose={onClose}>
      <div className="space-y-4">
        {budgetsQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
        {budgetsQuery.data && budgetsQuery.data.length === 0 && (
          <p className="text-sm text-slate-500">Nenhum teto cadastrado ainda.</p>
        )}
        {budgetsQuery.data && budgetsQuery.data.length > 0 && (
          <ul className="divide-y divide-slate-100 rounded-md border border-slate-200 dark:divide-slate-800 dark:border-slate-800">
            {budgetsQuery.data.map((budget) => (
              <li key={budget.id} className="flex items-center justify-between px-3 py-2 text-sm">
                <span>{budget.month ?? "Padrão (todo mês)"}</span>
                <span className="flex items-center gap-3">
                  {currencyFormatter.format(budget.limitAmount)}
                  <button
                    type="button"
                    onClick={() => deleteMutation.mutate(budget.id)}
                    className="text-red-600 hover:underline"
                  >
                    Remover
                  </button>
                </span>
              </li>
            ))}
          </ul>
        )}

        <form
          onSubmit={handleSubmit((data) => createMutation.mutate(data))}
          className="space-y-3 border-t border-slate-200 pt-4 dark:border-slate-800"
          noValidate
        >
          <FormField label="Competência (AAAA-MM, vazio = padrão)" htmlFor="budget-month" error={errors.month?.message}>
            <input id="budget-month" placeholder="2026-01" className={inputClass} {...register("month")} />
          </FormField>
          <FormField label="Teto (R$)" htmlFor="budget-limit" error={errors.limitAmount?.message}>
            <input
              id="budget-limit"
              type="number"
              step="0.01"
              className={inputClass}
              {...register("limitAmount")}
            />
          </FormField>
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
          >
            Adicionar
          </button>
        </form>
      </div>
    </Modal>
  );
}
