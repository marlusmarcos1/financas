import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { fetchAccounts } from "@/features/accounts/api";
import { fetchEmergencyReserve } from "@/features/networth/api";
import { ApiError } from "@/lib/api";
import {
  createGoal,
  createGoalContribution,
  deleteGoal,
  deleteGoalContribution,
  fetchGoalContributions,
  fetchGoals,
  updateGoal,
} from "./api";
import { contributionSourceLabels, goalTypeLabels, type Goal } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const goalSchema = z.object({
  name: z.string().min(1, "Informe o nome."),
  type: z.enum(["HOUSE", "CAR", "EMERGENCY", "OTHER"]),
  targetAmount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "Não pode ser negativo."),
  targetDate: z.string(),
  linkedAccountId: z.string(),
  monthlyContributionPlanned: z.coerce.number().min(0, "Não pode ser negativo."),
  priority: z.coerce.number().int().min(0),
  notes: z.string(),
});
type GoalFormData = z.infer<typeof goalSchema>;

export function GoalsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [isCreating, setIsCreating] = useState(false);
  const [editing, setEditing] = useState<Goal | null>(null);
  const [contributionsGoal, setContributionsGoal] = useState<Goal | null>(null);

  const goalsQuery = useQuery({ queryKey: ["goals"], queryFn: fetchGoals });
  const accountsQuery = useQuery({ queryKey: ["accounts"], queryFn: fetchAccounts });
  const reserveQuery = useQuery({ queryKey: ["emergency-reserve"], queryFn: fetchEmergencyReserve });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["goals"] });

  const toInput = (data: GoalFormData) => ({
    ...data,
    targetDate: data.targetDate || null,
    linkedAccountId: data.linkedAccountId || null,
  });

  const createMutation = useMutation({
    mutationFn: (data: GoalFormData) => createGoal(toInput(data)),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Meta criada.");
      setIsCreating(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao criar meta."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: GoalFormData }) => updateGoal(id, toInput(data)),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Meta atualizada.");
      setEditing(null);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao atualizar meta."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteGoal,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Meta excluída.");
    },
    onError: () => toast.showError("Erro ao excluir meta."),
  });

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Metas</h1>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
        >
          Nova meta
        </button>
      </div>

      {reserveQuery.data && (
        <div className="mb-6 rounded-xl border border-slate-200 p-4 dark:border-slate-800">
          <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
            Reserva de emergência
          </h2>
          <p className="text-sm text-slate-700 dark:text-slate-300">
            {currencyFormatter.format(reserveQuery.data.currentReserve)} de{" "}
            {currencyFormatter.format(reserveQuery.data.targetAmount)} (meta:{" "}
            {reserveQuery.data.emergencyMonthsTarget} meses de gastos) ·{" "}
            <strong>{reserveQuery.data.monthsOfCoverage.toFixed(1)} meses de cobertura</strong>
          </p>
        </div>
      )}

      {goalsQuery.data && goalsQuery.data.length === 0 && (
        <p className="text-sm text-slate-500">Nenhuma meta cadastrada ainda.</p>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {(goalsQuery.data ?? []).map((goal) => {
          const progressPercent = goal.targetAmount > 0 ? Math.min((goal.currentAmount / goal.targetAmount) * 100, 100) : 0;
          return (
            <div key={goal.id} className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
              <div className="mb-2 flex items-center justify-between">
                <div>
                  <p className="font-semibold text-slate-900 dark:text-slate-50">{goal.name}</p>
                  <p className="text-xs text-slate-500 dark:text-slate-400">{goalTypeLabels[goal.type]}</p>
                </div>
                <div className="flex gap-2 text-sm">
                  <button
                    type="button"
                    onClick={() => setContributionsGoal(goal)}
                    className="text-slate-600 hover:underline dark:text-slate-300"
                  >
                    Aportes
                  </button>
                  <button
                    type="button"
                    onClick={() => setEditing(goal)}
                    className="text-slate-600 hover:underline dark:text-slate-300"
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(`Excluir a meta "${goal.name}"?`)) deleteMutation.mutate(goal.id);
                    }}
                    className="text-red-600 hover:underline"
                  >
                    Excluir
                  </button>
                </div>
              </div>

              <div className="mb-1 flex justify-between text-sm">
                <span>{currencyFormatter.format(goal.currentAmount)}</span>
                <span className="text-slate-500 dark:text-slate-400">{currencyFormatter.format(goal.targetAmount)}</span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                <div className="h-2 rounded-full bg-emerald-500" style={{ width: `${progressPercent}%` }} />
              </div>

              <div className="mt-3 space-y-1 text-sm text-slate-600 dark:text-slate-300">
                {goal.targetDate && <p>Data-alvo: {goal.targetDate}</p>}
                {goal.monthsRemaining != null && <p>Meses restantes: {goal.monthsRemaining}</p>}
                {goal.requiredMonthlyContribution != null && (
                  <p>Aporte mensal necessário: {currencyFormatter.format(goal.requiredMonthlyContribution)}</p>
                )}
              </div>

              {goal.warning && (
                <p className="mt-3 rounded-md bg-amber-50 p-2 text-xs text-amber-900 dark:bg-amber-950 dark:text-amber-100">
                  {goal.warning}
                </p>
              )}
            </div>
          );
        })}
      </div>

      {isCreating && (
        <GoalFormModal
          title="Nova meta"
          accounts={accountsQuery.data ?? []}
          onClose={() => setIsCreating(false)}
          onSubmit={(data) => createMutation.mutate(data)}
        />
      )}

      {editing && (
        <GoalFormModal
          title="Editar meta"
          initialValues={editing}
          accounts={accountsQuery.data ?? []}
          onClose={() => setEditing(null)}
          onSubmit={(data) => updateMutation.mutate({ id: editing.id, data })}
        />
      )}

      {contributionsGoal && (
        <ContributionsModal goal={contributionsGoal} onClose={() => setContributionsGoal(null)} />
      )}
    </div>
  );
}

function GoalFormModal({
  title,
  initialValues,
  accounts,
  onClose,
  onSubmit,
}: {
  title: string;
  initialValues?: Goal;
  accounts: { id: string; name: string }[];
  onClose: () => void;
  onSubmit: (data: GoalFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<GoalFormData>({
    resolver: zodResolver(goalSchema),
    defaultValues: initialValues
      ? {
          name: initialValues.name,
          type: initialValues.type,
          targetAmount: initialValues.targetAmount,
          targetDate: initialValues.targetDate ?? "",
          linkedAccountId: initialValues.linkedAccountId ?? "",
          monthlyContributionPlanned: initialValues.monthlyContributionPlanned,
          priority: initialValues.priority,
          notes: initialValues.notes ?? "",
        }
      : {
          name: "",
          type: "HOUSE",
          targetAmount: 0,
          targetDate: "",
          linkedAccountId: "",
          monthlyContributionPlanned: 0,
          priority: 0,
          notes: "",
        },
  });

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Nome" htmlFor="goal-name" error={errors.name?.message}>
          <input id="goal-name" className={inputClass} {...register("name")} />
        </FormField>
        <FormField label="Tipo" htmlFor="goal-type" error={errors.type?.message}>
          <select id="goal-type" className={inputClass} {...register("type")}>
            {Object.entries(goalTypeLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </FormField>
        <div className="grid grid-cols-2 gap-4">
          <FormField label="Valor alvo (R$)" htmlFor="goal-amount" error={errors.targetAmount?.message}>
            <input id="goal-amount" type="number" step="0.01" className={inputClass} {...register("targetAmount")} />
          </FormField>
          <FormField label="Data-alvo (opcional)" htmlFor="goal-date" error={errors.targetDate?.message}>
            <input id="goal-date" type="date" className={inputClass} {...register("targetDate")} />
          </FormField>
        </div>
        <FormField label="Conta vinculada (opcional)" htmlFor="goal-account" error={errors.linkedAccountId?.message}>
          <select id="goal-account" className={inputClass} {...register("linkedAccountId")}>
            <option value="">Nenhuma</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </FormField>
        <FormField
          label="Aporte mensal planejado (R$)"
          htmlFor="goal-monthly"
          error={errors.monthlyContributionPlanned?.message}
        >
          <input
            id="goal-monthly"
            type="number"
            step="0.01"
            className={inputClass}
            {...register("monthlyContributionPlanned")}
          />
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

const contributionSchema = z.object({
  date: z.string().min(1, "Informe a data."),
  amount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).positive("Deve ser maior que zero."),
  source: z.enum(["SALARY", "SCHOLARSHIP", "THIRTEENTH", "EXTRA", "MANUAL"]),
});
type ContributionFormData = z.infer<typeof contributionSchema>;

function ContributionsModal({ goal, onClose }: { goal: Goal; onClose: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const contributionsQuery = useQuery({
    queryKey: ["goal-contributions", goal.id],
    queryFn: () => fetchGoalContributions(goal.id),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ContributionFormData>({
    resolver: zodResolver(contributionSchema),
    defaultValues: { date: new Date().toISOString().slice(0, 10), amount: 0, source: "MANUAL" },
  });

  const createMutation = useMutation({
    mutationFn: (data: ContributionFormData) => createGoalContribution(goal.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["goal-contributions", goal.id] });
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      toast.showSuccess("Aporte registrado.");
      reset({ date: new Date().toISOString().slice(0, 10), amount: 0, source: "MANUAL" });
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao registrar aporte."),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteGoalContribution(goal.id, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["goal-contributions", goal.id] });
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      toast.showSuccess("Aporte removido.");
    },
    onError: () => toast.showError("Erro ao remover aporte."),
  });

  return (
    <Modal title={`Aportes — ${goal.name}`} onClose={onClose}>
      <div className="space-y-4">
        {contributionsQuery.data && contributionsQuery.data.length > 0 && (
          <ul className="max-h-48 divide-y divide-slate-100 overflow-y-auto rounded-md border border-slate-200 dark:divide-slate-800 dark:border-slate-800">
            {contributionsQuery.data.map((contribution) => (
              <li key={contribution.id} className="flex items-center justify-between px-3 py-2 text-sm">
                <span>
                  {contribution.date} · {contributionSourceLabels[contribution.source]}
                </span>
                <span className="flex items-center gap-3">
                  {currencyFormatter.format(contribution.amount)}
                  <button
                    type="button"
                    onClick={() => deleteMutation.mutate(contribution.id)}
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
          <div className="grid grid-cols-2 gap-3">
            <FormField label="Data" htmlFor="contrib-date" error={errors.date?.message}>
              <input id="contrib-date" type="date" className={inputClass} {...register("date")} />
            </FormField>
            <FormField label="Valor (R$)" htmlFor="contrib-amount" error={errors.amount?.message}>
              <input id="contrib-amount" type="number" step="0.01" className={inputClass} {...register("amount")} />
            </FormField>
          </div>
          <FormField label="Origem" htmlFor="contrib-source" error={errors.source?.message}>
            <select id="contrib-source" className={inputClass} {...register("source")}>
              {Object.entries(contributionSourceLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
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
