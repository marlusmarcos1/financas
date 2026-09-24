import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { ApiError } from "@/lib/api";
import { fetchAvailableLimit, fetchInvoicesByCard, payInvoice } from "./api";
import { invoiceStatusLabels, type Invoice } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const payFormSchema = z.object({
  accountId: z.string().min(1, "Escolha a conta de pagamento."),
  amount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).positive("Deve ser maior que zero."),
  paidOn: z.string().min(1, "Informe a data do pagamento."),
});

type PayFormData = z.infer<typeof payFormSchema>;

export function InvoicesModal({
  cardId,
  cardName,
  accounts,
  onClose,
}: {
  cardId: string;
  cardName: string;
  accounts: { id: string; name: string }[];
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [payingInvoice, setPayingInvoice] = useState<Invoice | null>(null);

  const invoicesQuery = useQuery({
    queryKey: ["invoices", cardId],
    queryFn: () => fetchInvoicesByCard(cardId),
  });
  const limitQuery = useQuery({
    queryKey: ["credit-cards", cardId, "available-limit"],
    queryFn: () => fetchAvailableLimit(cardId),
  });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<PayFormData>({
    resolver: zodResolver(payFormSchema),
    defaultValues: { accountId: "", amount: 0, paidOn: new Date().toISOString().slice(0, 10) },
  });

  const payMutation = useMutation({
    mutationFn: (data: PayFormData) => payInvoice(payingInvoice!.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invoices", cardId] });
      queryClient.invalidateQueries({ queryKey: ["credit-cards", cardId, "available-limit"] });
      toast.showSuccess("Pagamento registrado.");
      setPayingInvoice(null);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao pagar fatura."),
  });

  return (
    <Modal title={`Faturas — ${cardName}`} onClose={onClose}>
      <div className="space-y-4">
        {limitQuery.data && (
          <p className="text-sm text-slate-600 dark:text-slate-300">
            Limite disponível: <strong>{currencyFormatter.format(limitQuery.data.availableLimit)}</strong>
          </p>
        )}

        {invoicesQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
        {invoicesQuery.data && invoicesQuery.data.length === 0 && (
          <p className="text-sm text-slate-500">Nenhuma fatura ainda.</p>
        )}

        {invoicesQuery.data && invoicesQuery.data.length > 0 && (
          <ul className="divide-y divide-slate-100 rounded-md border border-slate-200 dark:divide-slate-800 dark:border-slate-800">
            {invoicesQuery.data.map((invoice) => (
              <li key={invoice.id} className="flex items-center justify-between px-3 py-2 text-sm">
                <div>
                  <div className="font-medium">{invoice.referenceMonth}</div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    {invoiceStatusLabels[invoice.status]} · vence em {invoice.dueDate}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span>{currencyFormatter.format(invoice.total)}</span>
                  {invoice.status !== "PAID" && (
                    <button
                      type="button"
                      onClick={() => setPayingInvoice(invoice)}
                      className="text-slate-600 hover:underline dark:text-slate-300"
                    >
                      Pagar
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}

        {payingInvoice && (
          <form
            onSubmit={handleSubmit((data) => payMutation.mutate(data))}
            className="space-y-3 border-t border-slate-200 pt-4 dark:border-slate-800"
            noValidate
          >
            <p className="text-sm font-medium">Pagar fatura {payingInvoice.referenceMonth}</p>
            <FormField label="Conta de pagamento" htmlFor="pay-account" error={errors.accountId?.message}>
              <select id="pay-account" className={inputClass} {...register("accountId")}>
                <option value="">Selecione</option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.name}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label="Valor pago (R$)" htmlFor="pay-amount" error={errors.amount?.message}>
              <input id="pay-amount" type="number" step="0.01" className={inputClass} {...register("amount")} />
            </FormField>
            <FormField label="Data do pagamento" htmlFor="pay-date" error={errors.paidOn?.message}>
              <input id="pay-date" type="date" className={inputClass} {...register("paidOn")} />
            </FormField>
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
            >
              Confirmar pagamento
            </button>
          </form>
        )}
      </div>
    </Modal>
  );
}
