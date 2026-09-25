import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { useToast } from "@/components/ToastProvider";
import { ApiError } from "@/lib/api";
import { fetchRetirementPlan, fetchRetirementProjection, saveRetirementPlan } from "./api";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const planSchema = z.object({
  monthlyContribution: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "Não pode ser negativo."),
  contributionAnnualIncreasePercent: z.coerce.number().min(0, "Não pode ser negativo."),
  startDate: z.string().min(1, "Informe a data de início."),
  horizonYears: z.coerce.number().int().min(1, "Pelo menos 1 ano."),
  expectedReturnNominalAnnualPercent: z.coerce.number().min(0, "Não pode ser negativo."),
  expectedInflationAnnualPercent: z.coerce.number().min(0, "Não pode ser negativo."),
  currentBalance: z.coerce.number().min(0, "Não pode ser negativo."),
});
type PlanFormData = z.infer<typeof planSchema>;

export function RetirementPage() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const planQuery = useQuery({ queryKey: ["retirement-plan"], queryFn: fetchRetirementPlan });
  const projectionQuery = useQuery({ queryKey: ["retirement-projection"], queryFn: fetchRetirementProjection });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<PlanFormData>({
    resolver: zodResolver(planSchema),
    defaultValues: {
      monthlyContribution: 500,
      contributionAnnualIncreasePercent: 0,
      startDate: new Date().toISOString().slice(0, 10),
      horizonYears: 30,
      expectedReturnNominalAnnualPercent: 8,
      expectedInflationAnnualPercent: 4.5,
      currentBalance: 0,
    },
  });

  useEffect(() => {
    if (planQuery.data) {
      reset({
        monthlyContribution: planQuery.data.monthlyContribution,
        contributionAnnualIncreasePercent: planQuery.data.contributionAnnualIncreasePercent,
        startDate: planQuery.data.startDate,
        horizonYears: planQuery.data.horizonYears,
        expectedReturnNominalAnnualPercent: planQuery.data.expectedReturnNominalAnnual * 100,
        expectedInflationAnnualPercent: planQuery.data.expectedInflationAnnual * 100,
        currentBalance: planQuery.data.currentBalance,
      });
    }
  }, [planQuery.data, reset]);

  const saveMutation = useMutation({
    mutationFn: (data: PlanFormData) =>
      saveRetirementPlan({
        monthlyContribution: data.monthlyContribution,
        contributionAnnualIncreasePercent: data.contributionAnnualIncreasePercent,
        startDate: data.startDate,
        horizonYears: data.horizonYears,
        expectedReturnNominalAnnual: data.expectedReturnNominalAnnualPercent / 100,
        expectedInflationAnnual: data.expectedInflationAnnualPercent / 100,
        currentBalance: data.currentBalance,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["retirement-plan"] });
      queryClient.invalidateQueries({ queryKey: ["retirement-projection"] });
      toast.showSuccess("Plano de aposentadoria salvo.");
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao salvar plano."),
  });

  const baseScenario = projectionQuery.data?.scenarios.find((s) => s.label === "Base");
  const chartData = baseScenario?.yearlyBalances.map((snapshot, index) => {
    const row: Record<string, number> = { year: snapshot.year };
    projectionQuery.data?.scenarios.forEach((scenario) => {
      row[scenario.label] = scenario.yearlyBalances[index]?.balance ?? 0;
    });
    return row;
  });

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Aposentadoria</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          Aporte mensal, reajuste anual e projeção em 3 cenários (pessimista 6%, base 8%, otimista 10%).
        </p>
      </div>

      <form
        onSubmit={handleSubmit((data) => saveMutation.mutate(data))}
        className="max-w-2xl space-y-4 rounded-xl border border-slate-200 p-6 dark:border-slate-800"
        noValidate
      >
        <div className="grid grid-cols-2 gap-4">
          <FormField label="Aporte mensal (R$)" htmlFor="ret-contribution" error={errors.monthlyContribution?.message}>
            <input id="ret-contribution" type="number" step="0.01" className={inputClass} {...register("monthlyContribution")} />
          </FormField>
          <FormField
            label="Reajuste anual do aporte (%)"
            htmlFor="ret-increase"
            error={errors.contributionAnnualIncreasePercent?.message}
          >
            <input
              id="ret-increase"
              type="number"
              step="0.01"
              className={inputClass}
              {...register("contributionAnnualIncreasePercent")}
            />
          </FormField>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <FormField label="Data de início" htmlFor="ret-start" error={errors.startDate?.message}>
            <input id="ret-start" type="date" className={inputClass} {...register("startDate")} />
          </FormField>
          <FormField label="Horizonte (anos)" htmlFor="ret-horizon" error={errors.horizonYears?.message}>
            <input id="ret-horizon" type="number" className={inputClass} {...register("horizonYears")} />
          </FormField>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <FormField
            label="Retorno nominal esperado (% a.a.)"
            htmlFor="ret-return"
            error={errors.expectedReturnNominalAnnualPercent?.message}
          >
            <input
              id="ret-return"
              type="number"
              step="0.01"
              className={inputClass}
              {...register("expectedReturnNominalAnnualPercent")}
            />
          </FormField>
          <FormField
            label="Inflação esperada (% a.a.)"
            htmlFor="ret-inflation"
            error={errors.expectedInflationAnnualPercent?.message}
          >
            <input
              id="ret-inflation"
              type="number"
              step="0.01"
              className={inputClass}
              {...register("expectedInflationAnnualPercent")}
            />
          </FormField>
        </div>
        <FormField label="Saldo atual acumulado (R$)" htmlFor="ret-balance" error={errors.currentBalance?.message}>
          <input id="ret-balance" type="number" step="0.01" className={inputClass} {...register("currentBalance")} />
        </FormField>
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
        >
          Salvar plano
        </button>
      </form>

      {projectionQuery.data && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            {projectionQuery.data.scenarios.map((scenario) => (
              <div key={scenario.label} className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
                <p className="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">
                  {scenario.label} ({(scenario.annualRate * 100).toFixed(0)}% a.a.)
                </p>
                <p className="mt-1 text-xl font-semibold text-slate-900 dark:text-slate-50">
                  {currencyFormatter.format(scenario.finalBalanceNominal)}
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Valor real: {currencyFormatter.format(scenario.finalBalanceReal)}
                </p>
                <p className="mt-2 text-xs text-slate-600 dark:text-slate-300">
                  Aportado: {currencyFormatter.format(scenario.totalContributed)}
                  <br />
                  Juros: {currencyFormatter.format(scenario.interestEarned)}
                </p>
              </div>
            ))}
          </div>

          <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
            <h2 className="mb-4 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
              Evolução do patrimônio
            </h2>
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-slate-200 dark:stroke-slate-800" />
                  <XAxis dataKey="year" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => currencyFormatter.format(Number(v))} width={90} />
                  <Tooltip formatter={(value) => currencyFormatter.format(Number(value))} />
                  <Legend />
                  <Line type="monotone" dataKey="Pessimista" stroke="#f97316" strokeWidth={2} dot={false} />
                  <Line type="monotone" dataKey="Base" stroke="#0f172a" strokeWidth={2} dot={false} />
                  <Line type="monotone" dataKey="Otimista" stroke="#16a34a" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
