import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { Modal } from "@/components/Modal";
import { useToast } from "@/components/ToastProvider";
import { ApiError } from "@/lib/api";
import {
  archiveAccount,
  createAccount,
  fetchAccounts,
  restoreAccount,
  updateAccount,
} from "./api";
import { accountPurposeLabels, accountTypeLabels, type Account } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const accountSchema = z.object({
  name: z.string().min(1, "Informe o nome da conta."),
  type: z.enum(["CHECKING", "SAVINGS_BOX", "BROKERAGE", "CASH"]),
  institution: z.string(),
  initialBalance: z.coerce.number({ invalid_type_error: "Informe um valor válido." }),
  purpose: z.enum(["DAILY", "EMERGENCY_RESERVE", "GOAL", "INVESTMENT"]),
});

type AccountFormData = z.infer<typeof accountSchema>;

export function AccountsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editingAccount, setEditingAccount] = useState<Account | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const accountsQuery = useQuery({ queryKey: ["accounts"], queryFn: fetchAccounts });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["accounts"] });

  const createMutation = useMutation({
    mutationFn: createAccount,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Conta criada.");
      setIsCreating(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao criar conta."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, ...input }: { id: string } & AccountFormData) => updateAccount(id, input),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Conta atualizada.");
      setEditingAccount(null);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao atualizar conta."),
  });

  const archiveMutation = useMutation({
    mutationFn: archiveAccount,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Conta arquivada.");
    },
    onError: () => toast.showError("Erro ao arquivar conta."),
  });

  const restoreMutation = useMutation({
    mutationFn: restoreAccount,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Conta restaurada.");
    },
    onError: () => toast.showError("Erro ao restaurar conta."),
  });

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Contas</h1>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
        >
          Nova conta
        </button>
      </div>

      {accountsQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
      {accountsQuery.isError && <p className="text-sm text-red-600">Erro ao carregar contas.</p>}
      {accountsQuery.data && accountsQuery.data.length === 0 && (
        <p className="text-sm text-slate-500">Nenhuma conta cadastrada ainda.</p>
      )}

      {accountsQuery.data && accountsQuery.data.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Nome</th>
                <th className="px-4 py-2">Tipo</th>
                <th className="px-4 py-2">Finalidade</th>
                <th className="px-4 py-2">Saldo inicial</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {accountsQuery.data.map((account) => (
                <tr key={account.id} className={account.archived ? "opacity-60" : undefined}>
                  <td className="px-4 py-2 font-medium">{account.name}</td>
                  <td className="px-4 py-2">{accountTypeLabels[account.type]}</td>
                  <td className="px-4 py-2">{accountPurposeLabels[account.purpose]}</td>
                  <td className="px-4 py-2">{currencyFormatter.format(account.initialBalance)}</td>
                  <td className="px-4 py-2">{account.archived ? "Arquivada" : "Ativa"}</td>
                  <td className="px-4 py-2 text-right">
                    {!account.archived ? (
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => setEditingAccount(account)}
                          className="text-slate-600 hover:underline dark:text-slate-300"
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            if (window.confirm(`Arquivar a conta "${account.name}"?`)) {
                              archiveMutation.mutate(account.id);
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
                        onClick={() => restoreMutation.mutate(account.id)}
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
        <AccountFormModal
          title="Nova conta"
          onClose={() => setIsCreating(false)}
          onSubmit={(data) => createMutation.mutate(data)}
        />
      )}

      {editingAccount && (
        <AccountFormModal
          title="Editar conta"
          initialValues={editingAccount}
          onClose={() => setEditingAccount(null)}
          onSubmit={(data) => updateMutation.mutate({ id: editingAccount.id, ...data })}
        />
      )}
    </div>
  );
}

function AccountFormModal({
  title,
  initialValues,
  onClose,
  onSubmit,
}: {
  title: string;
  initialValues?: Account;
  onClose: () => void;
  onSubmit: (data: AccountFormData) => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AccountFormData>({
    resolver: zodResolver(accountSchema),
    defaultValues: initialValues
      ? {
          name: initialValues.name,
          type: initialValues.type,
          institution: initialValues.institution ?? "",
          initialBalance: initialValues.initialBalance,
          purpose: initialValues.purpose,
        }
      : { type: "CHECKING", purpose: "DAILY", institution: "", initialBalance: 0, name: "" },
  });

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField label="Nome" htmlFor="acc-name" error={errors.name?.message}>
          <input id="acc-name" className={inputClass} {...register("name")} />
        </FormField>

        <FormField label="Tipo" htmlFor="acc-type" error={errors.type?.message}>
          <select id="acc-type" className={inputClass} {...register("type")}>
            {Object.entries(accountTypeLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Instituição" htmlFor="acc-institution" error={errors.institution?.message}>
          <input id="acc-institution" className={inputClass} {...register("institution")} />
        </FormField>

        <FormField label="Saldo inicial (R$)" htmlFor="acc-balance" error={errors.initialBalance?.message}>
          <input
            id="acc-balance"
            type="number"
            step="0.01"
            className={inputClass}
            {...register("initialBalance")}
          />
        </FormField>

        <FormField label="Finalidade" htmlFor="acc-purpose" error={errors.purpose?.message}>
          <select id="acc-purpose" className={inputClass} {...register("purpose")}>
            {Object.entries(accountPurposeLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
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
