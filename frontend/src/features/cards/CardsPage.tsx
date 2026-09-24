import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { fetchAccounts } from "@/features/accounts/api";
import { InvoicesModal } from "@/features/invoices/InvoicesModal";
import { ApiError } from "@/lib/api";
import {
  archiveCreditCard,
  createCreditCard,
  fetchCreditCards,
  restoreCreditCard,
  updateCreditCard,
} from "./api";
import type { CreditCard } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const cardSchema = z.object({
  name: z.string().min(1, "Informe o nome do cartão."),
  issuer: z.string(),
  creditLimit: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "O limite não pode ser negativo."),
  closingDay: z.coerce.number().int().min(1, "Entre 1 e 31.").max(31, "Entre 1 e 31."),
  dueDay: z.coerce.number().int().min(1, "Entre 1 e 31.").max(31, "Entre 1 e 31."),
  defaultPaymentAccountId: z.string(),
  color: z.string(),
});

type CardFormData = z.infer<typeof cardSchema>;

export function CardsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editingCard, setEditingCard] = useState<CreditCard | null>(null);
  const [invoicesCard, setInvoicesCard] = useState<CreditCard | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const cardsQuery = useQuery({ queryKey: ["credit-cards"], queryFn: fetchCreditCards });
  const accountsQuery = useQuery({ queryKey: ["accounts"], queryFn: fetchAccounts });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["credit-cards"] });

  const toInput = (data: CardFormData) => ({
    ...data,
    defaultPaymentAccountId: data.defaultPaymentAccountId || null,
  });

  const createMutation = useMutation({
    mutationFn: (data: CardFormData) => createCreditCard(toInput(data)),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Cartão criado.");
      setIsCreating(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao criar cartão."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CardFormData }) => updateCreditCard(id, toInput(data)),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Cartão atualizado.");
      setEditingCard(null);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao atualizar cartão."),
  });

  const archiveMutation = useMutation({
    mutationFn: archiveCreditCard,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Cartão arquivado.");
    },
    onError: () => toast.showError("Erro ao arquivar cartão."),
  });

  const restoreMutation = useMutation({
    mutationFn: restoreCreditCard,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Cartão restaurado.");
    },
    onError: () => toast.showError("Erro ao restaurar cartão."),
  });

  const accountNameById = new Map((accountsQuery.data ?? []).map((account) => [account.id, account.name]));

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Cartões</h1>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
        >
          Novo cartão
        </button>
      </div>

      {cardsQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
      {cardsQuery.isError && <p className="text-sm text-red-600">Erro ao carregar cartões.</p>}
      {cardsQuery.data && cardsQuery.data.length === 0 && (
        <p className="text-sm text-slate-500">Nenhum cartão cadastrado ainda.</p>
      )}

      {cardsQuery.data && cardsQuery.data.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Nome</th>
                <th className="px-4 py-2">Limite</th>
                <th className="px-4 py-2">Fechamento</th>
                <th className="px-4 py-2">Vencimento</th>
                <th className="px-4 py-2">Conta padrão</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {cardsQuery.data.map((card) => (
                <tr key={card.id} className={card.archived ? "opacity-60" : undefined}>
                  <td className="px-4 py-2 font-medium">{card.name}</td>
                  <td className="px-4 py-2">{currencyFormatter.format(card.creditLimit)}</td>
                  <td className="px-4 py-2">dia {card.closingDay}</td>
                  <td className="px-4 py-2">dia {card.dueDay}</td>
                  <td className="px-4 py-2">
                    {card.defaultPaymentAccountId
                      ? (accountNameById.get(card.defaultPaymentAccountId) ?? "—")
                      : "—"}
                  </td>
                  <td className="px-4 py-2">{card.archived ? "Arquivado" : "Ativo"}</td>
                  <td className="px-4 py-2 text-right">
                    {!card.archived ? (
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => setInvoicesCard(card)}
                          className="text-slate-600 hover:underline dark:text-slate-300"
                        >
                          Faturas
                        </button>
                        <button
                          type="button"
                          onClick={() => setEditingCard(card)}
                          className="text-slate-600 hover:underline dark:text-slate-300"
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            if (window.confirm(`Arquivar o cartão "${card.name}"?`)) {
                              archiveMutation.mutate(card.id);
                            }
                          }}
                          className="text-red-600 hover:underline"
                        >
                          Arquivar
                        </button>
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => restoreMutation.mutate(card.id)}
                        className="text-slate-600 hover:underline dark:text-slate-300"
                      >
                        Restaurar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isCreating && (
        <CardFormModal
          title="Novo cartão"
          accounts={accountsQuery.data ?? []}
          onClose={() => setIsCreating(false)}
          onSubmit={(data) => createMutation.mutate(data)}
        />
      )}

      {editingCard && (
        <CardFormModal
          title="Editar cartão"
          initialValues={editingCard}
          accounts={accountsQuery.data ?? []}
          onClose={() => setEditingCard(null)}
          onSubmit={(data) => updateMutation.mutate({ id: editingCard.id, data })}
        />
      )}

      {invoicesCard && (
        <InvoicesModal
          cardId={invoicesCard.id}
          cardName={invoicesCard.name}
          accounts={accountsQuery.data ?? []}
          onClose={() => setInvoicesCard(null)}
        />
      )}
    </div>
  );
}

function CardFormModal({
  title,
  initialValues,
  accounts,
  onClose,
  onSubmit,
}: {
  title: string;
  initialValues?: CreditCard;
  accounts: { id: string; name: string }[];
  onClose: () => void;
  onSubmit: (data: CardFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CardFormData>({
    resolver: zodResolver(cardSchema),
    defaultValues: initialValues
      ? {
          name: initialValues.name,
          issuer: initialValues.issuer ?? "",
          creditLimit: initialValues.creditLimit,
          closingDay: initialValues.closingDay,
          dueDay: initialValues.dueDay,
          defaultPaymentAccountId: initialValues.defaultPaymentAccountId ?? "",
          color: initialValues.color ?? "",
        }
      : { name: "", issuer: "", creditLimit: 0, closingDay: 1, dueDay: 10, defaultPaymentAccountId: "", color: "" },
  });

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Nome" htmlFor="card-name" error={errors.name?.message}>
          <input id="card-name" className={inputClass} {...register("name")} />
        </FormField>

        <FormField label="Bandeira / emissor" htmlFor="card-issuer" error={errors.issuer?.message}>
          <input id="card-issuer" className={inputClass} {...register("issuer")} />
        </FormField>

        <FormField label="Limite (R$)" htmlFor="card-limit" error={errors.creditLimit?.message}>
          <input id="card-limit" type="number" step="0.01" className={inputClass} {...register("creditLimit")} />
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Dia de fechamento" htmlFor="card-closing" error={errors.closingDay?.message}>
            <input
              id="card-closing"
              type="number"
              min={1}
              max={31}
              className={inputClass}
              {...register("closingDay")}
            />
          </FormField>
          <FormField label="Dia de vencimento" htmlFor="card-due" error={errors.dueDay?.message}>
            <input id="card-due" type="number" min={1} max={31} className={inputClass} {...register("dueDay")} />
          </FormField>
        </div>

        <FormField
          label="Conta de pagamento padrão"
          htmlFor="card-account"
          error={errors.defaultPaymentAccountId?.message}
        >
          <select id="card-account" className={inputClass} {...register("defaultPaymentAccountId")}>
            <option value="">Nenhuma</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Cor (opcional)" htmlFor="card-color" error={errors.color?.message}>
          <input id="card-color" type="color" className="h-10 w-16" {...register("color")} />
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
