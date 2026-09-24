import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField, inputClass } from "@/components/FormField";
import { useToast } from "@/components/ToastProvider";
import { ApiError } from "@/lib/api";
import { changePassword, fetchSettings, updateSettings } from "./api";

const settingsSchema = z.object({
  tithePercent: z.coerce.number().min(0).max(100),
  installmentLimitPercent: z.coerce.number().min(0).max(100),
  emergencyMonthsTarget: z.coerce.number().int().min(1),
  currency: z.string().min(1, "Informe a moeda."),
});

type SettingsFormData = z.infer<typeof settingsSchema>;

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Informe a senha atual."),
    newPassword: z.string().min(6, "A nova senha deve ter pelo menos 6 caracteres."),
    confirmPassword: z.string().min(1, "Confirme a nova senha."),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "As senhas não coincidem.",
    path: ["confirmPassword"],
  });

type PasswordFormData = z.infer<typeof passwordSchema>;

export function SettingsPage() {
  return (
    <div className="max-w-xl space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Configurações</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          Parâmetros usados nos cálculos do app. Categorias ficam na aba "Categorias".
        </p>
      </div>
      <SettingsForm />
      <PasswordForm />
    </div>
  );
}

function SettingsForm() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const settingsQuery = useQuery({ queryKey: ["settings"], queryFn: fetchSettings });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<SettingsFormData>({ resolver: zodResolver(settingsSchema) });

  useEffect(() => {
    if (settingsQuery.data) {
      reset(settingsQuery.data);
    }
  }, [settingsQuery.data, reset]);

  const mutation = useMutation({
    mutationFn: updateSettings,
    onSuccess: (data) => {
      queryClient.setQueryData(["settings"], data);
      toast.showSuccess("Configurações salvas.");
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao salvar."),
  });

  if (settingsQuery.isLoading) {
    return <p className="text-sm text-slate-500">Carregando...</p>;
  }

  return (
    <section className="rounded-xl border border-slate-200 p-6 dark:border-slate-800">
      <h2 className="mb-4 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">Parâmetros</h2>
      <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-4" noValidate>
        <FormField label="Dízimo (%)" htmlFor="set-tithe" error={errors.tithePercent?.message}>
          <input id="set-tithe" type="number" step="0.1" className={inputClass} {...register("tithePercent")} />
        </FormField>
        <FormField
          label="Limite de comprometimento com parcelas (%)"
          htmlFor="set-installment"
          error={errors.installmentLimitPercent?.message}
        >
          <input
            id="set-installment"
            type="number"
            step="0.1"
            className={inputClass}
            {...register("installmentLimitPercent")}
          />
        </FormField>
        <FormField
          label="Meta de meses de reserva de emergência"
          htmlFor="set-emergency"
          error={errors.emergencyMonthsTarget?.message}
        >
          <input id="set-emergency" type="number" className={inputClass} {...register("emergencyMonthsTarget")} />
        </FormField>
        <FormField label="Moeda" htmlFor="set-currency" error={errors.currency?.message}>
          <input id="set-currency" className={inputClass} {...register("currency")} />
        </FormField>
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
        >
          Salvar parâmetros
        </button>
      </form>
    </section>
  );
}

function PasswordForm() {
  const toast = useToast();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<PasswordFormData>({ resolver: zodResolver(passwordSchema) });

  const mutation = useMutation({
    mutationFn: (data: PasswordFormData) =>
      changePassword({ currentPassword: data.currentPassword, newPassword: data.newPassword }),
    onSuccess: () => {
      toast.showSuccess("Senha alterada.");
      reset();
    },
    onError: (error) =>
      toast.showError(error instanceof ApiError ? error.message : "Erro ao trocar a senha."),
  });

  return (
    <section className="rounded-xl border border-slate-200 p-6 dark:border-slate-800">
      <h2 className="mb-4 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">Trocar senha</h2>
      <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-4" noValidate>
        <FormField label="Senha atual" htmlFor="pwd-current" error={errors.currentPassword?.message}>
          <input id="pwd-current" type="password" className={inputClass} {...register("currentPassword")} />
        </FormField>
        <FormField label="Nova senha" htmlFor="pwd-new" error={errors.newPassword?.message}>
          <input id="pwd-new" type="password" className={inputClass} {...register("newPassword")} />
        </FormField>
        <FormField label="Confirmar nova senha" htmlFor="pwd-confirm" error={errors.confirmPassword?.message}>
          <input id="pwd-confirm" type="password" className={inputClass} {...register("confirmPassword")} />
        </FormField>
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
        >
          Trocar senha
        </button>
      </form>
    </section>
  );
}
