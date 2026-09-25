import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { fetchCreditCards } from "@/features/cards/api";
import { ApiError } from "@/lib/api";
import { simulateInstallmentPurchase } from "./api";
import { verdictColors, verdictLabels } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const percentFormatter = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

const simulationSchema = z.object({
  description: z.string().min(1, "Informe a descrição."),
  totalAmount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).positive("Deve ser maior que zero."),
  installmentCount: z.coerce.number().int().min(1, "Pelo menos 1 parcela."),
  cardId: z.string().min(1, "Escolha o cartão."),
  purchaseDate: z.string().min(1, "Informe a data."),
  interestRatePercent: z.coerce.number().min(0, "Não pode ser negativo.").optional(),
});
type SimulationFormData = z.infer<typeof simulationSchema>;

export function SimulatorPage() {
  const cardsQuery = useQuery({ queryKey: ["credit-cards"], queryFn: fetchCreditCards });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SimulationFormData>({
    resolver: zodResolver(simulationSchema),
    defaultValues: {
      description: "",
      totalAmount: 0,
      installmentCount: 1,
      cardId: "",
      purchaseDate: new Date().toISOString().slice(0, 10),
    },
  });

  const simulateMutation = useMutation({
    mutationFn: (data: SimulationFormData) =>
      simulateInstallmentPurchase({
        description: data.description,
        totalAmount: data.totalAmount,
        installmentCount: data.installmentCount,
        cardId: data.cardId,
        purchaseDate: data.purchaseDate,
        interestRateMonthly: data.interestRatePercent ? data.interestRatePercent / 100 : undefined,
      }),
    onError: () => {
      // erro exibido abaixo do formulário via simulateMutation.error
    },
  });

  const result = simulateMutation.data;
  const chartData =
    result?.months.map((month) => ({
      month: month.month,
      "Saldo livre com a compra": month.freeBalance,
      "Saldo livre sem a compra": month.freeBalanceWithoutPurchase,
    })) ?? [];

  return (
    <div>
      <h1 className="mb-1 text-lg font-semibold text-slate-900 dark:text-slate-50">
        Simulador de compra parcelada segura
      </h1>
      <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
        Veja o impacto de uma nova compra parcelada no seu orçamento antes de fechar a compra.
      </p>

      <form
        onSubmit={handleSubmit((data) => simulateMutation.mutate(data))}
        className="mb-8 max-w-2xl space-y-4 rounded-xl border border-slate-200 p-6 dark:border-slate-800"
        noValidate
      >
        <FormField label="Descrição" htmlFor="sim-description" error={errors.description?.message}>
          <input id="sim-description" className={inputClass} {...register("description")} />
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Valor total (R$)" htmlFor="sim-total" error={errors.totalAmount?.message}>
            <input id="sim-total" type="number" step="0.01" className={inputClass} {...register("totalAmount")} />
          </FormField>
          <FormField label="Nº de parcelas" htmlFor="sim-count" error={errors.installmentCount?.message}>
            <input id="sim-count" type="number" min={1} className={inputClass} {...register("installmentCount")} />
          </FormField>
        </div>

        <FormField label="Cartão" htmlFor="sim-card" error={errors.cardId?.message}>
          <select id="sim-card" className={inputClass} {...register("cardId")}>
            <option value="">Selecione</option>
            {(cardsQuery.data ?? []).map((card) => (
              <option key={card.id} value={card.id}>
                {card.name}
              </option>
            ))}
          </select>
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Data da compra" htmlFor="sim-date" error={errors.purchaseDate?.message}>
            <input id="sim-date" type="date" className={inputClass} {...register("purchaseDate")} />
          </FormField>
          <FormField
            label="Juros ao mês (%, opcional)"
            htmlFor="sim-interest"
            error={errors.interestRatePercent?.message}
          >
            <input
              id="sim-interest"
              type="number"
              step="0.01"
              className={inputClass}
              {...register("interestRatePercent")}
            />
          </FormField>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
        >
          Simular
        </button>

        {simulateMutation.isError && (
          <p className="text-sm text-red-600">
            {simulateMutation.error instanceof ApiError
              ? simulateMutation.error.message
              : "Erro ao simular a compra."}
          </p>
        )}
      </form>

      {result && (
        <div className="space-y-6">
          <div className="flex flex-wrap items-center gap-4">
            <span className={`rounded-full px-4 py-1.5 text-sm font-semibold ${verdictColors[result.verdict]}`}>
              {verdictLabels[result.verdict]}
            </span>
            <span className="text-sm text-slate-600 dark:text-slate-300">
              Parcela: <strong>{currencyFormatter.format(result.installmentAmount)}</strong>/mês
            </span>
            <span className="text-sm text-slate-600 dark:text-slate-300">
              Você pode assumir com segurança até{" "}
              <strong>{currencyFormatter.format(result.maxSafeInstallmentToday)}</strong>/mês em novas parcelas hoje.
            </span>
          </div>

          {result.suggestions.length > 0 && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-100">
              <p className="mb-2 font-semibold">Alternativas para considerar:</p>
              <ul className="list-inside list-disc space-y-1">
                {result.suggestions.map((suggestion) => (
                  <li key={suggestion}>{suggestion}</li>
                ))}
              </ul>
            </div>
          )}

          <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
            <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
              Impacto no limite do cartão
            </h2>
            <p className="text-sm text-slate-700 dark:text-slate-300">
              Limite: {currencyFormatter.format(result.cardLimitImpact.creditLimit)} · Disponível hoje:{" "}
              {currencyFormatter.format(result.cardLimitImpact.availableLimitBefore)} · Disponível após a compra:{" "}
              {currencyFormatter.format(result.cardLimitImpact.availableLimitAfter)}
            </p>
          </div>

          <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
            <h2 className="mb-4 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
              Saldo livre mês a mês — com e sem a compra
            </h2>
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-slate-200 dark:stroke-slate-800" />
                  <XAxis dataKey="month" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => currencyFormatter.format(Number(v))} width={90} />
                  <Tooltip formatter={(value) => currencyFormatter.format(Number(value))} />
                  <Legend />
                  <ReferenceLine y={0} stroke="#94a3b8" />
                  <Line type="monotone" dataKey="Saldo livre sem a compra" stroke="#94a3b8" strokeWidth={2} dot={false} />
                  <Line type="monotone" dataKey="Saldo livre com a compra" stroke="#0f172a" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-2">Mês</th>
                  <th className="px-4 py-2">Parcelas (com a compra)</th>
                  <th className="px-4 py-2">Comprometimento</th>
                  <th className="px-4 py-2">Saldo livre</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {result.months.map((month) => (
                  <tr key={month.month}>
                    <td className="px-4 py-2 font-medium">{month.month}</td>
                    <td className="px-4 py-2">{currencyFormatter.format(month.totalCommittedInstallments)}</td>
                    <td className="px-4 py-2">{percentFormatter.format(month.commitmentPercent)}%</td>
                    <td
                      className={`px-4 py-2 ${month.freeBalance < 0 ? "font-semibold text-red-600" : ""}`}
                    >
                      {currencyFormatter.format(month.freeBalance)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
