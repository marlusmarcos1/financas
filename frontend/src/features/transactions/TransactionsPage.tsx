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
import {
  createTransaction,
  deleteTransaction,
  fetchTransactions,
  markTransactionAsPaid,
  updateTransaction,
  type TransactionFilters,
} from "./api";
import { transactionKindLabels, transactionStatusLabels, type Transaction } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dateFormatter = new Intl.DateTimeFormat("pt-BR");

function formatDate(iso: string): string {
  const [year, month, day] = iso.split("-").map(Number);
  return dateFormatter.format(new Date(year, month - 1, day));
}

const transactionSchema = z
  .object({
    kind: z.enum(["INCOME", "EXPENSE", "TRANSFER"]),
    description: z.string().min(1, "Informe a descrição."),
    amount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "Não pode ser negativo."),
    date: z.string().min(1, "Informe a data."),
    categoryId: z.string(),
    accountId: z.string(),
    cardId: z.string(),
    status: z.enum(["PLANNED", "PAID"]),
    notes: z.string(),
  })
  .refine(
    (data) => {
      if (data.kind === "EXPENSE") {
        return (data.accountId !== "") !== (data.cardId !== "");
      }
      return data.accountId !== "" && data.cardId === "";
    },
    {
      message:
        "Despesa: escolha conta OU cartão, não os dois. Receita/transferência: escolha só a conta.",
      path: ["accountId"],
    },
  );

type TransactionFormData = z.infer<typeof transactionSchema>;

export function TransactionsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const transactionsQuery = useQuery({
    queryKey: ["transactions", filters],
    queryFn: () => fetchTransactions(filters),
  });
  const accountsQuery = useQuery({ queryKey: ["accounts"], queryFn: fetchAccounts });
  const cardsQuery = useQuery({ queryKey: ["credit-cards"], queryFn: fetchCreditCards });
  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: fetchCategories });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["transactions"] });

  const createMutation = useMutation({
    mutationFn: createTransaction,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Lançamento criado.");
      setIsCreating(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao criar lançamento."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Parameters<typeof createTransaction>[0] }) =>
      updateTransaction(id, data),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Lançamento atualizado.");
      setEditing(null);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao atualizar."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteTransaction,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Lançamento excluído.");
    },
    onError: () => toast.showError("Erro ao excluir lançamento."),
  });

  const markPaidMutation = useMutation({
    mutationFn: markTransactionAsPaid,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Lançamento marcado como pago.");
    },
    onError: () => toast.showError("Erro ao marcar como pago."),
  });

  const accounts = accountsQuery.data ?? [];
  const cards = cardsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];
  const accountNameById = new Map(accounts.map((a) => [a.id, a.name]));
  const cardNameById = new Map(cards.map((c) => [c.id, c.name]));
  const categoryNameById = new Map(categories.map((c) => [c.id, c.name]));

  const toInput = (data: TransactionFormData) => ({
    kind: data.kind,
    description: data.description,
    amount: data.amount,
    date: data.date,
    categoryId: data.categoryId || null,
    accountId: data.accountId || null,
    cardId: data.cardId || null,
    status: data.status,
    notes: data.notes,
  });

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Lançamentos</h1>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
        >
          Novo lançamento
        </button>
      </div>

      <div className="mb-4 flex flex-wrap gap-3">
        <input
          type="date"
          className={inputClass + " w-auto"}
          value={filters.from ?? ""}
          onChange={(e) => setFilters((f) => ({ ...f, from: e.target.value || undefined }))}
        />
        <input
          type="date"
          className={inputClass + " w-auto"}
          value={filters.to ?? ""}
          onChange={(e) => setFilters((f) => ({ ...f, to: e.target.value || undefined }))}
        />
        <select
          className={inputClass + " w-auto"}
          value={filters.categoryId ?? ""}
          onChange={(e) => setFilters((f) => ({ ...f, categoryId: e.target.value || undefined }))}
        >
          <option value="">Todas as categorias</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <select
          className={inputClass + " w-auto"}
          value={filters.status ?? ""}
          onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value || undefined }))}
        >
          <option value="">Todos os status</option>
          {Object.entries(transactionStatusLabels).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {transactionsQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
      {transactionsQuery.isError && <p className="text-sm text-red-600">Erro ao carregar lançamentos.</p>}
      {transactionsQuery.data && transactionsQuery.data.content.length === 0 && (
        <p className="text-sm text-slate-500">Nenhum lançamento encontrado.</p>
      )}

      {transactionsQuery.data && transactionsQuery.data.content.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Data</th>
                <th className="px-4 py-2">Descrição</th>
                <th className="px-4 py-2">Tipo</th>
                <th className="px-4 py-2">Categoria</th>
                <th className="px-4 py-2">Conta/Cartão</th>
                <th className="px-4 py-2">Valor</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {transactionsQuery.data.content.map((transaction) => (
                <tr key={transaction.id}>
                  <td className="px-4 py-2">{formatDate(transaction.date)}</td>
                  <td className="px-4 py-2 font-medium">{transaction.description}</td>
                  <td className="px-4 py-2">{transactionKindLabels[transaction.kind]}</td>
                  <td className="px-4 py-2">
                    {transaction.categoryId ? (categoryNameById.get(transaction.categoryId) ?? "—") : "—"}
                  </td>
                  <td className="px-4 py-2">
                    {transaction.accountId
                      ? (accountNameById.get(transaction.accountId) ?? "—")
                      : transaction.cardId
                        ? (cardNameById.get(transaction.cardId) ?? "—")
                        : "—"}
                  </td>
                  <td className="px-4 py-2">{currencyFormatter.format(transaction.amount)}</td>
                  <td className="px-4 py-2">{transactionStatusLabels[transaction.status]}</td>
                  <td className="px-4 py-2 text-right">
                    <div className="flex justify-end gap-2">
                      {transaction.status === "PLANNED" && (
                        <button
                          type="button"
                          onClick={() => markPaidMutation.mutate(transaction.id)}
                          className="text-slate-600 hover:underline dark:text-slate-300"
                        >
                          Marcar paga
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => setEditing(transaction)}
                        className="text-slate-600 hover:underline dark:text-slate-300"
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (window.confirm(`Excluir o lançamento "${transaction.description}"?`)) {
                            deleteMutation.mutate(transaction.id);
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
        <TransactionFormModal
          title="Novo lançamento"
          accounts={accounts}
          cards={cards}
          categories={categories}
          onClose={() => setIsCreating(false)}
          onSubmit={(data) => createMutation.mutate(toInput(data))}
        />
      )}

      {editing && (
        <TransactionFormModal
          title="Editar lançamento"
          initialValues={editing}
          accounts={accounts}
          cards={cards}
          categories={categories}
          onClose={() => setEditing(null)}
          onSubmit={(data) => updateMutation.mutate({ id: editing.id, data: toInput(data) })}
        />
      )}
    </div>
  );
}

function TransactionFormModal({
  title,
  initialValues,
  accounts,
  cards,
  categories,
  onClose,
  onSubmit,
}: {
  title: string;
  initialValues?: Transaction;
  accounts: { id: string; name: string }[];
  cards: { id: string; name: string }[];
  categories: { id: string; name: string }[];
  onClose: () => void;
  onSubmit: (data: TransactionFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<TransactionFormData>({
    resolver: zodResolver(transactionSchema),
    defaultValues: initialValues
      ? {
          kind: initialValues.kind,
          description: initialValues.description,
          amount: initialValues.amount,
          date: initialValues.date,
          categoryId: initialValues.categoryId ?? "",
          accountId: initialValues.accountId ?? "",
          cardId: initialValues.cardId ?? "",
          status: initialValues.status,
          notes: initialValues.notes ?? "",
        }
      : {
          kind: "EXPENSE",
          description: "",
          amount: 0,
          date: new Date().toISOString().slice(0, 10),
          categoryId: "",
          accountId: "",
          cardId: "",
          status: "PLANNED",
          notes: "",
        },
  });

  const kind = watch("kind");

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Tipo" htmlFor="tx-kind" error={errors.kind?.message}>
          <select id="tx-kind" className={inputClass} {...register("kind")}>
            {Object.entries(transactionKindLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Descrição" htmlFor="tx-description" error={errors.description?.message}>
          <input id="tx-description" className={inputClass} {...register("description")} />
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Valor (R$)" htmlFor="tx-amount" error={errors.amount?.message}>
            <input id="tx-amount" type="number" step="0.01" className={inputClass} {...register("amount")} />
          </FormField>
          <FormField label="Data" htmlFor="tx-date" error={errors.date?.message}>
            <input id="tx-date" type="date" className={inputClass} {...register("date")} />
          </FormField>
        </div>

        <FormField label="Categoria" htmlFor="tx-category" error={errors.categoryId?.message}>
          <select id="tx-category" className={inputClass} {...register("categoryId")}>
            <option value="">Nenhuma</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Conta" htmlFor="tx-account" error={errors.accountId?.message}>
          <select id="tx-account" className={inputClass} {...register("accountId")}>
            <option value="">Nenhuma</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </FormField>

        {kind === "EXPENSE" && (
          <FormField label="Cartão (alternativa à conta)" htmlFor="tx-card">
            <select id="tx-card" className={inputClass} {...register("cardId")}>
              <option value="">Nenhum</option>
              {cards.map((card) => (
                <option key={card.id} value={card.id}>
                  {card.name}
                </option>
              ))}
            </select>
          </FormField>
        )}

        <FormField label="Status" htmlFor="tx-status" error={errors.status?.message}>
          <select id="tx-status" className={inputClass} {...register("status")}>
            {Object.entries(transactionStatusLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Notas (opcional)" htmlFor="tx-notes" error={errors.notes?.message}>
          <input id="tx-notes" className={inputClass} {...register("notes")} />
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
