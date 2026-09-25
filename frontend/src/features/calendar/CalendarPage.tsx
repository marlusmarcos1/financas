import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { inputClass } from "@/components/FormField";
import { fetchCalendar } from "./api";
import type { CalendarItem } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dateFormatter = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" });

const typeLabels: Record<CalendarItem["type"], string> = {
  INVOICE: "Fatura",
  INSTALLMENT: "Parcela",
  RECURRING: "Recorrência",
};

const typeColors: Record<CalendarItem["type"], string> = {
  INVOICE: "bg-sky-100 text-sky-700 dark:bg-sky-900 dark:text-sky-200",
  INSTALLMENT: "bg-violet-100 text-violet-700 dark:bg-violet-900 dark:text-violet-200",
  RECURRING: "bg-amber-100 text-amber-700 dark:bg-amber-900 dark:text-amber-200",
};

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function CalendarPage() {
  const [horizonDays, setHorizonDays] = useState(30);

  const { from, to } = useMemo(() => {
    const today = new Date();
    const end = new Date();
    end.setDate(today.getDate() + horizonDays);
    return { from: toIsoDate(today), to: toIsoDate(end) };
  }, [horizonDays]);

  const calendarQuery = useQuery({ queryKey: ["calendar", from, to], queryFn: () => fetchCalendar(from, to) });

  const groupedByDate = useMemo(() => {
    const groups = new Map<string, CalendarItem[]>();
    for (const item of calendarQuery.data ?? []) {
      const list = groups.get(item.date) ?? [];
      list.push(item);
      groups.set(item.date, list);
    }
    return Array.from(groups.entries()).sort(([a], [b]) => a.localeCompare(b));
  }, [calendarQuery.data]);

  const total = (calendarQuery.data ?? []).reduce((sum, item) => sum + item.amount, 0);

  return (
    <div className="max-w-3xl">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Calendário de vencimentos</h1>
        <select
          className={inputClass + " w-auto"}
          value={horizonDays}
          onChange={(event) => setHorizonDays(Number(event.target.value))}
        >
          <option value={7}>Próximos 7 dias</option>
          <option value={15}>Próximos 15 dias</option>
          <option value={30}>Próximos 30 dias</option>
          <option value={60}>Próximos 60 dias</option>
        </select>
      </div>

      {calendarQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}

      {calendarQuery.data && calendarQuery.data.length === 0 && (
        <p className="text-sm text-slate-500">Nenhum vencimento no período selecionado.</p>
      )}

      {calendarQuery.data && calendarQuery.data.length > 0 && (
        <div className="space-y-6">
          <p className="text-sm text-slate-600 dark:text-slate-300">
            Total no período: <span className="font-semibold">{currencyFormatter.format(total)}</span>
          </p>
          {groupedByDate.map(([date, items]) => (
            <div key={date}>
              <h2 className="mb-2 text-sm font-semibold text-slate-700 dark:text-slate-300">
                {dateFormatter.format(new Date(date + "T00:00:00"))}
              </h2>
              <ul className="divide-y divide-slate-200 rounded-lg border border-slate-200 dark:divide-slate-800 dark:border-slate-800">
                {items.map((item, index) => (
                  <li key={`${item.type}-${item.description}-${index}`} className="flex items-center justify-between px-4 py-2 text-sm">
                    <div className="flex items-center gap-2">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${typeColors[item.type]}`}>
                        {typeLabels[item.type]}
                      </span>
                      <span>{item.description}</span>
                    </div>
                    <span className="font-medium text-slate-800 dark:text-slate-100">
                      {currencyFormatter.format(item.amount)}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
