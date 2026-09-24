import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { fetchAccounts } from "@/features/accounts/api";
import { ApiError } from "@/lib/api";
import {
  createIncomeEntry,
  createIncomeSource,
  deleteIncomeEntry,
  deleteIncomeSource,
  fetchIncomeEntries,
  fetchIncomeSources,
} from "./api";
import {
  incomeEntryStatusLabels,
  incomeRecurrenceLabels,
  incomeSourceTypeLabels,
  type IncomeSource,
} from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const sourceSchema = z.object({
  name: z.string().min(1, "Informe o nome."),
  type: z.enum(["SALARY", "SCHOLARSHIP", "THIRTEENTH", "EXTRA", "OTHER"]),
  recurrence: z.enum(["MONTHLY", "TEMPORARY", "SPORADIC"]),
  expectedAmount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "Não pode ser negativo."),
  titheApplies: z.boolean(),
  countsInBaseBudget: z.boolean(),
});
type SourceFormData = z.infer<typeof sourceSchema>;

const entrySchema = z.object({
  sourceId: z.string().min(1, "Escolha a fonte."),
  accountId: z.string(),
  referenceMonth: z.string().min(1, "Informe a competência."),
  receivedOn: z.string(),
  amount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "Não pode ser negativo."),
  status: z.enum(["EXPECTED", "RECEIVED"]),
});
type EntryFormData = z.infer<typeof entrySchema>;

export function IncomePage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const [isCreatingSource, setIsCreatingSource] = useState(false);
  const [isCreatingEntry, setIsCreatingEntry] = useState(false);

  const sourcesQuery = useQuery({ queryKey: ["income-sources"], queryFn: fetchIncomeSources });
  const entriesQuery = useQuery({
    queryKey: ["income-entries", month],
    queryFn: () => fetchIncomeEntries(month),
  });
  const accountsQuery = useQuery({ queryKey: ["accounts"], queryFn: fetchAccounts });

  const sources = sourcesQuery.data ?? [];
  const sourceNameById = new Map(sources.map((s) => [s.id, s.name]));

  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: ["income-sources"] });
    queryClient.invalidateQueries({ queryKey: ["income-entries"] });
    queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    queryClient.invalidateQueries({ queryKey: ["tithe-ledger"] });
  };

  const createSourceMutation = useMutation({
    mutationFn: createIncomeSource,
    onSuccess: () => {
      invalidateAll();
      toast.showSuccess("Fonte de receita criada.");
      setIsCreatingSource(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao criar fonte."),
  });

  const deleteSourceMutation = useMutation({
    mutationFn: deleteIncomeSource,
    onSuccess: () => {
      invalidateAll();
      toast.showSuccess("Fonte excluída.");
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao excluir fonte."),
  });

  const createEntryMutation = useMutation({
    mutationFn: createIncomeEntry,
    onSuccess: () => {
      invalidateAll();
      toast.showSuccess("Receita lançada.");
      setIsCreatingEntry(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao lançar receita."),
  });

  const deleteEntryMutation = useMutation({
    mutationFn: deleteIncomeEntry,
    onSuccess: () => {
      invalidateAll();
      toast.showSuccess("Lançamento removido.");
    },
    onError: () => toast.showError("Erro ao remover lançamento."),
  });

  return (
    <div className="space-y-8">
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Fontes de receita</h1>
          <button
            type="button"
            onClick={() => setIsCreatingSource(true)}
            className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
          >
            Nova fonte
          </button>
        </div>

        {sources.length === 0 && <p className="text-sm text-slate-500">Nenhuma fonte cadastrada ainda.</p>}
        {sources.length > 0 && (
          <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-2">Nome</th>
                  <th className="px-4 py-2">Tipo</th>
                  <th className="px-4 py-2">Recorrência</th>
                  <th className="px-4 py-2">Valor esperado</th>
                  <th className="px-4 py-2">Entra na renda base?</th>
                  <th className="px-4 py-2">Gera dízimo?</th>
                  <th className="px-4 py-2" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {sources.map((source) => (
                  <tr key={source.id}>
                    <td className="px-4 py-2 font-medium">{source.name}</td>
                    <td className="px-4 py-2">{incomeSourceTypeLabels[source.type]}</td>
                    <td className="px-4 py-2">{incomeRecurrenceLabels[source.recurrence]}</td>
                    <td className="px-4 py-2">{currencyFormatter.format(source.expectedAmount)}</td>
                    <td className="px-4 py-2">{source.countsInBaseBudget ? "Sim" : "Não"}</td>
                    <td className="px-4 py-2">{source.titheApplies ? "Sim" : "Não"}</td>
                    <td className="px-4 py-2 text-right">
                      <button
                        type="button"
                        onClick={() => {
                          if (window.confirm(`Excluir a fonte "${source.name}"?`)) {
                            deleteSourceMutation.mutate(source.id);
                          }
                        }}
                        className="text-red-600 hover:underline"
                      >
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Receitas do mês</h2>
          <div className="flex items-center gap-3">
            <input
              type="month"
              className={inputClass + " w-auto"}
              value={month}
              onChange={(e) => setMonth(e.target.value)}
            />
            <button
              type="button"
              onClick={() => setIsCreatingEntry(true)}
              className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
            >
              Nova receita
            </button>
          </div>
        </div>

        {entriesQuery.data && entriesQuery.data.length === 0 && (
          <p className="text-sm text-slate-500">Nenhuma receita lançada nesta competência.</p>
        )}
        {entriesQuery.data && entriesQuery.data.length > 0 && (
          <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-2">Fonte</th>
                  <th className="px-4 py-2">Valor</th>
                  <th className="px-4 py-2">Status</th>
                  <th className="px-4 py-2">Recebido em</th>
                  <th className="px-4 py-2" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {entriesQuery.data.map((entry) => (
                  <tr key={entry.id}>
                    <td className="px-4 py-2 font-medium">{sourceNameById.get(entry.sourceId) ?? "—"}</td>
                    <td className="px-4 py-2">{currencyFormatter.format(entry.amount)}</td>
                    <td className="px-4 py-2">{incomeEntryStatusLabels[entry.status]}</td>
                    <td className="px-4 py-2">{entry.receivedOn ?? "—"}</td>
                    <td className="px-4 py-2 text-right">
                      <button
                        type="button"
                        onClick={() => deleteEntryMutation.mutate(entry.id)}
                        className="text-red-600 hover:underline"
                      >
                        Remover
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {isCreatingSource && (
        <SourceFormModal
          onClose={() => setIsCreatingSource(false)}
          onSubmit={(data) =>
            createSourceMutation.mutate({
              ...data,
              payDay: null,
              startDate: null,
              endDate: null,
              expectedMonths: null,
            })
          }
        />
      )}

      {isCreatingEntry && (
        <EntryFormModal
          sources={sources}
          accounts={accountsQuery.data ?? []}
          defaultMonth={month}
          onClose={() => setIsCreatingEntry(false)}
          onSubmit={(data) =>
            createEntryMutation.mutate({
              ...data,
              accountId: data.accountId || null,
              receivedOn: data.receivedOn || null,
            })
          }
        />
      )}
    </div>
  );
}

function SourceFormModal({
  onClose,
  onSubmit,
}: {
  onClose: () => void;
  onSubmit: (data: SourceFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SourceFormData>({
    resolver: zodResolver(sourceSchema),
    defaultValues: {
      name: "",
      type: "SALARY",
      recurrence: "MONTHLY",
      expectedAmount: 0,
      titheApplies: true,
      countsInBaseBudget: false,
    },
  });

  return (
    <Modal title="Nova fonte de receita" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Nome" htmlFor="src-name" error={errors.name?.message}>
          <input id="src-name" className={inputClass} {...register("name")} />
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Tipo" htmlFor="src-type" error={errors.type?.message}>
            <select id="src-type" className={inputClass} {...register("type")}>
              {Object.entries(incomeSourceTypeLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Recorrência" htmlFor="src-recurrence" error={errors.recurrence?.message}>
            <select id="src-recurrence" className={inputClass} {...register("recurrence")}>
              {Object.entries(incomeRecurrenceLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </FormField>
        </div>

        <FormField label="Valor esperado (R$)" htmlFor="src-amount" error={errors.expectedAmount?.message}>
          <input id="src-amount" type="number" step="0.01" className={inputClass} {...register("expectedAmount")} />
        </FormField>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...register("countsInBaseBudget")} />
          Entra na renda base (usada no teto de parcelas e orçamento do mês)
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...register("titheApplies")} />
          Gera dízimo
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

function EntryFormModal({
  sources,
  accounts,
  defaultMonth,
  onClose,
  onSubmit,
}: {
  sources: IncomeSource[];
  accounts: { id: string; name: string }[];
  defaultMonth: string;
  onClose: () => void;
  onSubmit: (data: EntryFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EntryFormData>({
    resolver: zodResolver(entrySchema),
    defaultValues: {
      sourceId: "",
      accountId: "",
      referenceMonth: defaultMonth,
      receivedOn: new Date().toISOString().slice(0, 10),
      amount: 0,
      status: "RECEIVED",
    },
  });

  return (
    <Modal title="Nova receita" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Fonte" htmlFor="entry-source" error={errors.sourceId?.message}>
          <select id="entry-source" className={inputClass} {...register("sourceId")}>
            <option value="">Selecione</option>
            {sources.map((source) => (
              <option key={source.id} value={source.id}>
                {source.name}
              </option>
            ))}
          </select>
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Competência" htmlFor="entry-month" error={errors.referenceMonth?.message}>
            <input id="entry-month" type="month" className={inputClass} {...register("referenceMonth")} />
          </FormField>
          <FormField label="Valor (R$)" htmlFor="entry-amount" error={errors.amount?.message}>
            <input id="entry-amount" type="number" step="0.01" className={inputClass} {...register("amount")} />
          </FormField>
        </div>

        <FormField label="Conta de destino (opcional)" htmlFor="entry-account" error={errors.accountId?.message}>
          <select id="entry-account" className={inputClass} {...register("accountId")}>
            <option value="">Nenhuma</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Status" htmlFor="entry-status" error={errors.status?.message}>
            <select id="entry-status" className={inputClass} {...register("status")}>
              {Object.entries(incomeEntryStatusLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Recebido em" htmlFor="entry-received" error={errors.receivedOn?.message}>
            <input id="entry-received" type="date" className={inputClass} {...register("receivedOn")} />
          </FormField>
        </div>

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
