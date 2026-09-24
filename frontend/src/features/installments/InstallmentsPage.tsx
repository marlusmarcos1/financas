import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { fetchCreditCards } from "@/features/cards/api";
import { fetchCategories } from "@/features/categories/api";
import { ApiError } from "@/lib/api";
import { createInstallmentPlan, deleteInstallmentPlan, fetchInstallmentPlans } from "./api";
import type { InstallmentPlanInput } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const percentFormatter = new Intl.NumberFormat("pt-BR", { style: "percent", minimumFractionDigits: 2 });

const planSchema = z
  .object({
    cardId: z.string().min(1, "Escolha o cartão."),
    description: z.string().min(1, "Informe a descrição."),
    purchaseDate: z.string().min(1, "Informe a data da compra."),
    totalAmount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).positive("Deve ser maior que zero."),
    installmentCount: z.coerce.number().int().min(1, "Pelo menos 1 parcela."),
    interestRatePercent: z.coerce.number().min(0, "Não pode ser negativo.").optional(),
    categoryId: z.string(),
    inProgress: z.boolean(),
    firstInstallmentNumber: z.coerce.number().int().min(1).optional(),
    firstInvoiceMonth: z.string().optional(),
  })
  .refine((data) => !data.inProgress || (data.firstInstallmentNumber && data.firstInvoiceMonth), {
    message: "Informe em qual parcela você está e a competência da fatura dela.",
    path: ["firstInstallmentNumber"],
  });

type PlanFormData = z.infer<typeof planSchema>;

export function InstallmentsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [isCreating, setIsCreating] = useState(false);

  const plansQuery = useQuery({ queryKey: ["installment-plans"], queryFn: fetchInstallmentPlans });
  const cardsQuery = useQuery({ queryKey: ["credit-cards"], queryFn: fetchCreditCards });
  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: fetchCategories });

  const cards = cardsQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];
  const cardNameById = new Map(cards.map((c) => [c.id, c.name]));

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["installment-plans"] });
    queryClient.invalidateQueries({ queryKey: ["transactions"] });
    queryClient.invalidateQueries({ queryKey: ["commitments"] });
  };

  const createMutation = useMutation({
    mutationFn: (input: InstallmentPlanInput) => createInstallmentPlan(input),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Parcelamento criado e parcelas lançadas.");
      setIsCreating(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao criar parcelamento."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteInstallmentPlan,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Parcelamento excluído.");
    },
    onError: () => toast.showError("Erro ao excluir parcelamento."),
  });

  const toInput = (data: PlanFormData): InstallmentPlanInput => ({
    cardId: data.cardId,
    description: data.description,
    purchaseDate: data.purchaseDate,
    totalAmount: data.totalAmount,
    installmentCount: data.installmentCount,
    categoryId: data.categoryId || null,
    ...(data.interestRatePercent
      ? { interestRateMonthly: data.interestRatePercent / 100 }
      : {}),
    ...(data.inProgress
      ? { firstInstallmentNumber: data.firstInstallmentNumber, firstInvoiceMonth: data.firstInvoiceMonth }
      : {}),
  });

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Parcelamentos</h1>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
        >
          Novo parcelamento
        </button>
      </div>

      {plansQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
      {plansQuery.data && plansQuery.data.length === 0 && (
        <p className="text-sm text-slate-500">Nenhum parcelamento cadastrado ainda.</p>
      )}

      {plansQuery.data && plansQuery.data.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Descrição</th>
                <th className="px-4 py-2">Cartão</th>
                <th className="px-4 py-2">Parcela</th>
                <th className="px-4 py-2">Valor da parcela</th>
                <th className="px-4 py-2">Termina em</th>
                <th className="px-4 py-2">Juros</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {plansQuery.data.map((plan) => (
                <tr key={plan.id}>
                  <td className="px-4 py-2 font-medium">{plan.description}</td>
                  <td className="px-4 py-2">{cardNameById.get(plan.cardId) ?? "—"}</td>
                  <td className="px-4 py-2">
                    {plan.firstInstallmentNumber}/{plan.installmentCount}
                  </td>
                  <td className="px-4 py-2">{currencyFormatter.format(plan.installmentAmount)}</td>
                  <td className="px-4 py-2">{plan.lastInstallmentMonth}</td>
                  <td className="px-4 py-2">
                    {plan.effectiveRate != null ? percentFormatter.format(plan.effectiveRate) : "—"}
                  </td>
                  <td className="px-4 py-2 text-right">
                    <button
                      type="button"
                      onClick={() => {
                        if (window.confirm(`Excluir o parcelamento "${plan.description}"?`)) {
                          deleteMutation.mutate(plan.id);
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

      {isCreating && (
        <PlanFormModal
          cards={cards}
          categories={categories}
          onClose={() => setIsCreating(false)}
          onSubmit={(data) => createMutation.mutate(toInput(data))}
        />
      )}
    </div>
  );
}

function PlanFormModal({
  cards,
  categories,
  onClose,
  onSubmit,
}: {
  cards: { id: string; name: string }[];
  categories: { id: string; name: string }[];
  onClose: () => void;
  onSubmit: (data: PlanFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<PlanFormData>({
    resolver: zodResolver(planSchema),
    defaultValues: {
      cardId: "",
      description: "",
      purchaseDate: new Date().toISOString().slice(0, 10),
      totalAmount: 0,
      installmentCount: 1,
      categoryId: "",
      inProgress: false,
    },
  });

  const inProgress = watch("inProgress");

  return (
    <Modal title="Novo parcelamento" onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Descrição" htmlFor="plan-description" error={errors.description?.message}>
          <input id="plan-description" className={inputClass} {...register("description")} />
        </FormField>

        <FormField label="Cartão" htmlFor="plan-card" error={errors.cardId?.message}>
          <select id="plan-card" className={inputClass} {...register("cardId")}>
            <option value="">Selecione</option>
            {cards.map((card) => (
              <option key={card.id} value={card.id}>
                {card.name}
              </option>
            ))}
          </select>
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Valor total (R$)" htmlFor="plan-total" error={errors.totalAmount?.message}>
            <input id="plan-total" type="number" step="0.01" className={inputClass} {...register("totalAmount")} />
          </FormField>
          <FormField label="Nº de parcelas" htmlFor="plan-count" error={errors.installmentCount?.message}>
            <input id="plan-count" type="number" min={1} className={inputClass} {...register("installmentCount")} />
          </FormField>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Data da compra" htmlFor="plan-date" error={errors.purchaseDate?.message}>
            <input id="plan-date" type="date" className={inputClass} {...register("purchaseDate")} />
          </FormField>
          <FormField
            label="Juros ao mês (%, opcional)"
            htmlFor="plan-interest"
            error={errors.interestRatePercent?.message}
          >
            <input
              id="plan-interest"
              type="number"
              step="0.01"
              className={inputClass}
              {...register("interestRatePercent")}
            />
          </FormField>
        </div>

        <FormField label="Categoria (opcional)" htmlFor="plan-category" error={errors.categoryId?.message}>
          <select id="plan-category" className={inputClass} {...register("categoryId")}>
            <option value="">Nenhuma</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </FormField>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...register("inProgress")} />
          Parcelamento já em andamento (algumas parcelas já foram pagas fora do sistema)
        </label>

        {inProgress && (
          <div className="grid grid-cols-2 gap-4">
            <FormField
              label="Estou na parcela nº"
              htmlFor="plan-first-number"
              error={errors.firstInstallmentNumber?.message}
            >
              <input
                id="plan-first-number"
                type="number"
                min={1}
                className={inputClass}
                {...register("firstInstallmentNumber")}
              />
            </FormField>
            <FormField
              label="Competência dessa parcela"
              htmlFor="plan-first-month"
              error={errors.firstInvoiceMonth?.message}
            >
              <input id="plan-first-month" type="month" className={inputClass} {...register("firstInvoiceMonth")} />
            </FormField>
          </div>
        )}

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
