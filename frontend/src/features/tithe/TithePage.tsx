import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { ApiError } from "@/lib/api";
import { fetchTitheLedger, payTithe } from "./api";
import type { TitheLedgerEntry } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const paySchema = z.object({
  amount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).positive("Deve ser maior que zero."),
  paidOn: z.string().min(1, "Informe a data."),
});
type PayFormData = z.infer<typeof paySchema>;

export function TithePage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [paying, setPaying] = useState<TitheLedgerEntry | null>(null);

  const ledgerQuery = useQuery({ queryKey: ["tithe-ledger"], queryFn: fetchTitheLedger });

  const payMutation = useMutation({
    mutationFn: (data: PayFormData) => payTithe(paying!.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tithe-ledger"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      toast.showSuccess("Pagamento de dízimo registrado.");
      setPaying(null);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao registrar pagamento."),
  });

  const entries = ledgerQuery.data ?? [];
  const yearTotal = entries.reduce((sum, entry) => sum + entry.dueAmount, 0);

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Dízimo</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Acumulado nos meses listados: {currencyFormatter.format(yearTotal)}
        </p>
      </div>

      {entries.length === 0 && (
        <p className="text-sm text-slate-500">
          Nenhum registro ainda — o dízimo é calculado automaticamente quando você lança receitas recebidas.
        </p>
      )}

      {entries.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Competência</th>
                <th className="px-4 py-2">Base</th>
                <th className="px-4 py-2">Devido ({"%"})</th>
                <th className="px-4 py-2">Pago</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {entries.map((entry) => (
                <tr key={entry.id}>
                  <td className="px-4 py-2 font-medium">{entry.referenceMonth}</td>
                  <td className="px-4 py-2">{currencyFormatter.format(entry.baseAmount)}</td>
                  <td className="px-4 py-2">
                    {currencyFormatter.format(entry.dueAmount)} ({entry.percent}%)
                  </td>
                  <td className="px-4 py-2">{currencyFormatter.format(entry.paidAmount)}</td>
                  <td className="px-4 py-2">{entry.status === "PAID" ? "Pago" : "Pendente"}</td>
                  <td className="px-4 py-2 text-right">
                    {entry.status !== "PAID" && (
                      <button
                        type="button"
                        onClick={() => setPaying(entry)}
                        className="text-slate-600 hover:underline dark:text-slate-300"
                      >
                        Marcar pago
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {paying && <PayModal entry={paying} onClose={() => setPaying(null)} onSubmit={(data) => payMutation.mutate(data)} />}
    </div>
  );
}

function PayModal({
  entry,
  onClose,
  onSubmit,
}: {
  entry: TitheLedgerEntry;
  onClose: () => void;
  onSubmit: (data: PayFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<PayFormData>({
    resolver: zodResolver(paySchema),
    defaultValues: {
      amount: entry.dueAmount - entry.paidAmount,
      paidOn: new Date().toISOString().slice(0, 10),
    },
  });

  return (
    <Modal title={`Pagar dízimo — ${entry.referenceMonth}`} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Valor pago (R$)" htmlFor="tithe-amount" error={errors.amount?.message}>
          <input id="tithe-amount" type="number" step="0.01" className={inputClass} {...register("amount")} />
        </FormField>
        <FormField label="Data do pagamento" htmlFor="tithe-date" error={errors.paidOn?.message}>
          <input id="tithe-date" type="date" className={inputClass} {...register("paidOn")} />
        </FormField>
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
        >
          Confirmar
        </button>
      </form>
    </Modal>
  );
}
