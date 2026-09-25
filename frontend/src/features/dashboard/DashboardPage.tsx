import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { inputClass } from "@/components/FormField";
import { fetchNetWorth, fetchNetWorthHistory } from "@/features/networth/api";
import { fetchAlerts, fetchDashboard, fetchDashboardExtras } from "./api";

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
  const netWorthHistoryQuery = useQuery({ queryKey: ["net-worth-history"], queryFn: () => fetchNetWorthHistory(6) });
  const alertsQuery = useQuery({ queryKey: ["alerts"], queryFn: fetchAlerts });
  const extrasQuery = useQuery({ queryKey: ["dashboard-extras", month], queryFn: () => fetchDashboardExtras(month) });

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

      {alertsQuery.data && alertsQuery.data.length > 0 && (
        <div className="mb-4 space-y-2">
          {alertsQuery.data.map((alert, index) => (
            <div
              key={`${alert.severity}-${index}`}
              className={`rounded-md px-4 py-2 text-sm ${
                alert.severity === "DANGER"
                  ? "bg-red-50 text-red-800 dark:bg-red-950 dark:text-red-200"
                  : "bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-200"
              }`}
            >
              {alert.message}
            </div>
          ))}
        </div>
      )}

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

          {extrasQuery.data && (
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
                <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
                  Pague-se primeiro
                </h2>
                <ul className="space-y-2 text-sm">
                  {extrasQuery.data.payYourselfFirst.map((item) => (
                    <li key={item.label} className="flex items-center justify-between">
                      <span className="flex items-center gap-2">
                        <span
                          className={`inline-block h-2 w-2 rounded-full ${item.done ? "bg-emerald-500" : "bg-slate-300 dark:bg-slate-600"}`}
                        />
                        {item.label}
                      </span>
                      <span className="text-slate-600 dark:text-slate-300">
                        {currencyFormatter.format(item.targetAmount)}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>

              {extrasQuery.data.scholarships.length > 0 && (
                <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
                  <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
                    Contagem regressiva
                  </h2>
                  <ul className="space-y-2 text-sm">
                    {extrasQuery.data.scholarships.map((s) => (
                      <li key={s.sourceId} className="flex items-center justify-between">
                        <span>{s.name}</span>
                        <span className="text-slate-600 dark:text-slate-300">
                          {s.remainingMonths} mês(es) restante(s) (até {s.endsOn})
                        </span>
                      </li>
                    ))}
                  </ul>
                  <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">
                    A sobra do mês acima já considera só a renda base — é o cenário "e se a bolsa
                    acabasse agora".
                  </p>
                </div>
              )}
            </div>
          )}

          {netWorthHistoryQuery.data && netWorthHistoryQuery.data.length > 1 && (
            <div className="rounded-xl border border-slate-200 p-4 dark:border-slate-800">
              <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
                Evolução patrimonial (6 meses)
              </h2>
              <p className="mb-2 text-xs text-slate-500 dark:text-slate-400">
                Investimentos aqui somam o capital aportado (custo), não a cotação histórica — o
                app não tem cotação automática.
              </p>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={netWorthHistoryQuery.data}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="month" fontSize={12} />
                    <YAxis fontSize={12} tickFormatter={(v) => currencyFormatter.format(Number(v))} width={90} />
                    <Tooltip formatter={(value) => currencyFormatter.format(Number(value))} />
                    <Line type="monotone" dataKey="total" name="Patrimônio" stroke="#0f172a" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
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
