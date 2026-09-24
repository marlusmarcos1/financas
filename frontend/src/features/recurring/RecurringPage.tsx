import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { fetchAccounts } from "@/features/accounts/api";
import { fetchCreditCards } from "@/features/cards/api";
import { fetchCategories } from "@/features/categories/api";
import { ApiError } from "@/lib/api";
import { createRecurringRule, deleteRecurringRule, fetchRecurringRules, updateRecurringRule } from "./api";
import { recurringFrequencyLabels, type RecurringRule } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const recurringSchema = z
  .object({
    description: z.string().min(1, "Informe a descrição."),
    amount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "Não pode ser negativo."),
    amountIsVariable: z.boolean(),
    frequency: z.enum(["MONTHLY", "YEARLY"]),
    dayOfMonth: z.coerce.number().int().min(1, "Entre 1 e 31.").max(31, "Entre 1 e 31."),
    startDate: z.string().min(1, "Informe a data de início."),
    endDate: z.string(),
    categoryId: z.string().min(1, "Escolha a categoria."),
    cardId: z.string(),
    accountId: z.string(),
    active: z.boolean(),
  })
  .refine((data) => (data.cardId !== "") !== (data.accountId !== ""), {
    message: "Escolha conta OU cartão, não os dois.",
    path: ["accountId"],
  });

type RecurringFormData = z.infer<typeof recurringSchema>;

export function RecurringPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editing, setEditing] = useState<RecurringRule | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const rulesQuery = useQuery({ queryKey: ["recurring-rules"], queryFn: fetchRecurringRules });
  const accountsQuery = useQuery({ queryKey: ["accounts"], queryFn: fetchAccounts });
  const cardsQuery = useQuery({ queryKey: ["credit-cards"], queryFn: fetchCreditCards });
  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: fetchCategories });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["recurring-rules"] });

  const toInput = (data: RecurringFormData) => ({
    ...data,
    endDate: data.endDate || null,
    cardId: data.cardId || null,
    accountId: data.accountId || null,
  });

  const createMutation = useMutation({
    mutationFn: (data: RecurringFormData) => createRecurringRule(toInput(data)),
    onSuccess: () => {
      invalidate();
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      toast.showSuccess("Recorrência criada e lançamentos futuros gerados.");
      setIsCreating(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao criar recorrência."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: RecurringFormData }) => updateRecurringRule(id, toInput(data)),
    onSuccess: () => {
      invalidate();
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      toast.showSuccess("Recorrência atualizada.");
      setEditing(null);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao atualizar."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteRecurringRule,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Recorrência excluída.");
    },
    onError: () => toast.showError("Erro ao excluir recorrência."),
  });

  const accounts = accountsQuery.data ?? [];
  const cards = cardsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];
  const accountNameById = new Map(accounts.map((a) => [a.id, a.name]));
  const cardNameById = new Map(cards.map((c) => [c.id, c.name]));

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Recorrências</h1>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
        >
          Nova recorrência
        </button>
      </div>
      <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
        Assinaturas e contas fixas (Meli+, internet, crédito de celular...). Ao criar, os lançamentos dos
        próximos 12 meses são gerados automaticamente.
      </p>

      {rulesQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
      {rulesQuery.data && rulesQuery.data.length === 0 && (
        <p className="text-sm text-slate-500">Nenhuma recorrência cadastrada ainda.</p>
      )}

      {rulesQuery.data && rulesQuery.data.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Descrição</th>
                <th className="px-4 py-2">Valor</th>
                <th className="px-4 py-2">Frequência</th>
                <th className="px-4 py-2">Dia</th>
                <th className="px-4 py-2">Conta/Cartão</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {rulesQuery.data.map((rule) => (
                <tr key={rule.id} className={!rule.active ? "opacity-60" : undefined}>
                  <td className="px-4 py-2 font-medium">{rule.description}</td>
                  <td className="px-4 py-2">
                    {currencyFormatter.format(rule.amount)}
                    {rule.amountIsVariable && " (variável)"}
                  </td>
                  <td className="px-4 py-2">{recurringFrequencyLabels[rule.frequency]}</td>
                  <td className="px-4 py-2">dia {rule.dayOfMonth}</td>
                  <td className="px-4 py-2">
                    {rule.accountId
                      ? (accountNameById.get(rule.accountId) ?? "—")
                      : rule.cardId
                        ? (cardNameById.get(rule.cardId) ?? "—")
                        : "—"}
                  </td>
                  <td className="px-4 py-2">{rule.active ? "Ativa" : "Inativa"}</td>
                  <td className="px-4 py-2 text-right">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => setEditing(rule)}
                        className="text-slate-600 hover:underline dark:text-slate-300"
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (window.confirm(`Excluir a recorrência "${rule.description}"?`)) {
                            deleteMutation.mutate(rule.id);
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
        <RecurringFormModal
          title="Nova recorrência"
          accounts={accounts}
          cards={cards}
          categories={categories}
          onClose={() => setIsCreating(false)}
          onSubmit={(data) => createMutation.mutate(data)}
        />
      )}

      {editing && (
        <RecurringFormModal
          title="Editar recorrência"
          initialValues={editing}
          accounts={accounts}
          cards={cards}
          categories={categories}
          onClose={() => setEditing(null)}
          onSubmit={(data) => updateMutation.mutate({ id: editing.id, data })}
        />
      )}
    </div>
  );
}

function RecurringFormModal({
  title,
  initialValues,
  accounts,
  cards,
  categories,
  onClose,
  onSubmit,
}: {
  title: string;
  initialValues?: RecurringRule;
  accounts: { id: string; name: string }[];
  cards: { id: string; name: string }[];
  categories: { id: string; name: string }[];
  onClose: () => void;
  onSubmit: (data: RecurringFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RecurringFormData>({
    resolver: zodResolver(recurringSchema),
    defaultValues: initialValues
      ? {
          description: initialValues.description,
          amount: initialValues.amount,
          amountIsVariable: initialValues.amountIsVariable,
          frequency: initialValues.frequency,
          dayOfMonth: initialValues.dayOfMonth,
          startDate: initialValues.startDate,
          endDate: initialValues.endDate ?? "",
          categoryId: initialValues.categoryId,
          cardId: initialValues.cardId ?? "",
          accountId: initialValues.accountId ?? "",
          active: initialValues.active,
        }
      : {
          description: "",
          amount: 0,
          amountIsVariable: false,
          frequency: "MONTHLY",
          dayOfMonth: 5,
          startDate: new Date().toISOString().slice(0, 10),
          endDate: "",
          categoryId: "",
          cardId: "",
          accountId: "",
          active: true,
        },
  });

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Descrição" htmlFor="rec-description" error={errors.description?.message}>
          <input id="rec-description" className={inputClass} {...register("description")} />
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Valor (R$)" htmlFor="rec-amount" error={errors.amount?.message}>
            <input id="rec-amount" type="number" step="0.01" className={inputClass} {...register("amount")} />
          </FormField>
          <FormField label="Dia do mês" htmlFor="rec-day" error={errors.dayOfMonth?.message}>
            <input id="rec-day" type="number" min={1} max={31} className={inputClass} {...register("dayOfMonth")} />
          </FormField>
        </div>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...register("amountIsVariable")} />
          Valor variável (ex.: internet, combustível)
        </label>

        <FormField label="Frequência" htmlFor="rec-frequency" error={errors.frequency?.message}>
          <select id="rec-frequency" className={inputClass} {...register("frequency")}>
            {Object.entries(recurringFrequencyLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Início" htmlFor="rec-start" error={errors.startDate?.message}>
            <input id="rec-start" type="date" className={inputClass} {...register("startDate")} />
          </FormField>
          <FormField label="Fim (opcional)" htmlFor="rec-end" error={errors.endDate?.message}>
            <input id="rec-end" type="date" className={inputClass} {...register("endDate")} />
          </FormField>
        </div>

        <FormField label="Categoria" htmlFor="rec-category" error={errors.categoryId?.message}>
          <select id="rec-category" className={inputClass} {...register("categoryId")}>
            <option value="">Selecione</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Conta" htmlFor="rec-account" error={errors.accountId?.message}>
          <select id="rec-account" className={inputClass} {...register("accountId")}>
            <option value="">Nenhuma</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Cartão (alternativa à conta)" htmlFor="rec-card">
          <select id="rec-card" className={inputClass} {...register("cardId")}>
            <option value="">Nenhum</option>
            {cards.map((card) => (
              <option key={card.id} value={card.id}>
                {card.name}
              </option>
            ))}
          </select>
        </FormField>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...register("active")} />
          Ativa
        </label>

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
