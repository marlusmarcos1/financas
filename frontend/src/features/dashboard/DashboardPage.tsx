import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { inputClass } from "@/components/FormField";
import { fetchNetWorth } from "@/features/networth/api";
import { fetchDashboard } from "./api";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const statusColors: Record<string, string> = {
  GREEN: "bg-emerald-500",
  YELLOW: "bg-amber-500",
  RED: "bg-red-500",
};

export function DashboardPage() {
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const dashboardQuery = useQuery({ queryKey: ["dashboard", month], queryFn: () => fetchDashboard(month) });
  const netWorthQuery = useQuery({ queryKey: ["net-worth"], queryFn: fetchNetWorth });

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Dashboard</h1>
        <input
          type="month"
          className={inputClass + " w-auto"}
          value={month}
          onChange={(e) => setMonth(e.target.value)}
        />
      </div>

      {dashboardQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}

      {dashboardQuery.data && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <StatCard label="Renda base recebida" value={dashboardQuery.data.baseIncomeReceived} />
            <StatCard label="Extras recebidos" value={dashboardQuery.data.extrasReceived} />
            <StatCard label="Despesas do mês" value={dashboardQuery.data.totalExpenses} />
            <StatCard
              label="Sobra do mês"
              value={dashboardQuery.data.surplus}
              highlight={dashboardQuery.data.surplus < 0 ? "negative" : "positive"}
            />
          </div>

          {netWorthQuery.data && (
            <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
              <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
                Patrimônio total
              </h2>
              <p className="text-lg font-semibold text-slate-900 dark:text-slate-50">
                {currencyFormatter.format(netWorthQuery.data.netWorth)}
              </p>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Contas: {currencyFormatter.format(netWorthQuery.data.accountsTotal)} · Investimentos:{" "}
                {currencyFormatter.format(netWorthQuery.data.investmentsTotal)}
              </p>
            </div>
          )}

          <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
            <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
              Dízimo do mês
            </h2>
            <p className="text-sm text-slate-700 dark:text-slate-300">
              Devido: {currencyFormatter.format(dashboardQuery.data.titheDue)} · Pendente:{" "}
              {currencyFormatter.format(dashboardQuery.data.titheOutstanding)}
            </p>
          </div>

          <div>
            <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
              Orçamento por categoria
            </h2>
            {dashboardQuery.data.budgets.length === 0 && (
              <p className="text-sm text-slate-500">Nenhum teto de orçamento configurado ainda.</p>
            )}
            <div className="space-y-3">
              {dashboardQuery.data.budgets.map((budget) => (
                <div key={budget.categoryId}>
                  <div className="mb-1 flex justify-between text-sm">
                    <span className="font-medium">{budget.categoryName}</span>
                    <span className="text-slate-500 dark:text-slate-400">
                      {currencyFormatter.format(budget.spent)} / {currencyFormatter.format(budget.limitAmount)} (
                      {budget.percent.toFixed(0)}%)
                    </span>
                  </div>
                  <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                    <div
                      className={`h-2 rounded-full ${statusColors[budget.status]}`}
                      style={{ width: `${Math.min(budget.percent, 100)}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  highlight,
}: {
  label: string;
  value: number;
  highlight?: "positive" | "negative";
}) {
  const color =
    highlight === "negative"
      ? "text-red-600"
      : highlight === "positive"
        ? "text-emerald-600"
        : "text-slate-900 dark:text-slate-50";
  return (
    <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
      <p className="text-xs uppercase text-slate-500 dark:text-slate-400">{label}</p>
      <p className={`mt-1 text-lg font-semibold ${color}`}>{currencyFormatter.format(value)}</p>
    </div>
  );
}
