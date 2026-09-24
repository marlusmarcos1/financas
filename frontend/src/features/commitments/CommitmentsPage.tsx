import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { inputClass } from "@/components/FormField";
import { fetchCommitments } from "./api";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

function formatMonth(month: string): string {
  const [year, m] = month.split("-").map(Number);
  return new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" }).format(new Date(year, m - 1, 1));
}

export function CommitmentsPage() {
  const [months, setMonths] = useState(12);
  const commitmentsQuery = useQuery({
    queryKey: ["commitments", months],
    queryFn: () => fetchCommitments(months),
  });

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Compromissos futuros</h1>
        <select
          className={inputClass + " w-auto"}
          value={months}
          onChange={(e) => setMonths(Number(e.target.value))}
        >
          <option value={12}>Próximos 12 meses</option>
          <option value={24}>Próximos 24 meses</option>
        </select>
      </div>
      <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
        Quanto de parcelamentos e recorrências já está comprometido em cada mês, e quando cada
        parcelamento termina (liberando o valor mensal a partir do mês seguinte).
      </p>

      {commitmentsQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}

      {commitmentsQuery.data && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Mês</th>
                <th className="px-4 py-2">Parcelas</th>
                <th className="px-4 py-2">Recorrências</th>
                <th className="px-4 py-2">Total comprometido</th>
                <th className="px-4 py-2">Termina neste mês (libera)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {commitmentsQuery.data.map((row) => (
                <tr key={row.month}>
                  <td className="px-4 py-2 font-medium capitalize">{formatMonth(row.month)}</td>
                  <td className="px-4 py-2">{currencyFormatter.format(row.installmentsTotal)}</td>
                  <td className="px-4 py-2">{currencyFormatter.format(row.recurringTotal)}</td>
                  <td className="px-4 py-2 font-semibold">{currencyFormatter.format(row.committedTotal)}</td>
                  <td className="px-4 py-2">
                    {row.endingPlans.length === 0
                      ? "—"
                      : row.endingPlans
                          .map((plan) => `${plan.description} (${currencyFormatter.format(plan.monthlyAmount)})`)
                          .join(", ")}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
