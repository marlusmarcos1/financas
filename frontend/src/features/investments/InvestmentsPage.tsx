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
  archiveInvestmentAsset,
  createInvestmentAsset,
  createInvestmentTransaction,
  deleteInvestmentTransaction,
  fetchInvestmentAssets,
  fetchInvestmentTransactions,
  fetchPortfolio,
  updateAssetPrice,
} from "./api";
import { assetClassLabels, investmentPurposeLabels, transactionTypeLabels, type InvestmentAsset } from "./types";

const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const percentFormatter = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 });

const assetSchema = z.object({
  ticker: z.string().min(1, "Informe o ticker."),
  name: z.string().min(1, "Informe o nome."),
  assetClass: z.enum(["FII", "STOCK", "ETF", "TREASURY", "CDB", "SAVINGS_BOX", "CRYPTO", "OTHER"]),
  purpose: z.enum(["RETIREMENT", "HOUSE", "GENERAL"]),
});
type AssetFormData = z.infer<typeof assetSchema>;

export function InvestmentsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [isCreatingAsset, setIsCreatingAsset] = useState(false);
  const [operationsAsset, setOperationsAsset] = useState<InvestmentAsset | null>(null);

  const portfolioQuery = useQuery({ queryKey: ["portfolio"], queryFn: fetchPortfolio });
  const assetsQuery = useQuery({ queryKey: ["investment-assets"], queryFn: fetchInvestmentAssets });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["portfolio"] });
    queryClient.invalidateQueries({ queryKey: ["investment-assets"] });
    queryClient.invalidateQueries({ queryKey: ["net-worth"] });
  };

  const createAssetMutation = useMutation({
    mutationFn: (data: AssetFormData) =>
      createInvestmentAsset({ ...data, subclass: "", indexer: "", maturityDate: null }),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Ativo criado.");
      setIsCreatingAsset(false);
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao criar ativo."),
  });

  const archiveMutation = useMutation({
    mutationFn: archiveInvestmentAsset,
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Ativo arquivado.");
    },
    onError: () => toast.showError("Erro ao arquivar ativo."),
  });

  const priceMutation = useMutation({
    mutationFn: ({ id, price }: { id: string; price: number }) => updateAssetPrice(id, price),
    onSuccess: () => {
      invalidate();
      toast.showSuccess("Preço atualizado.");
    },
    onError: () => toast.showError("Erro ao atualizar preço."),
  });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AssetFormData>({
    resolver: zodResolver(assetSchema),
    defaultValues: { ticker: "", name: "", assetClass: "FII", purpose: "RETIREMENT" },
  });

  const assets = assetsQuery.data ?? [];
  const positions = portfolioQuery.data?.positions ?? [];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Investimentos</h1>
        <button
          type="button"
          onClick={() => setIsCreatingAsset(true)}
          className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 dark:bg-slate-100 dark:text-slate-900"
        >
          Novo ativo
        </button>
      </div>

      {portfolioQuery.data && (
        <p className="mb-4 text-sm text-slate-600 dark:text-slate-300">
          Valor total da carteira: <strong>{currencyFormatter.format(portfolioQuery.data.totalCurrentValue)}</strong>
        </p>
      )}

      {positions.length === 0 && <p className="text-sm text-slate-500">Nenhum ativo com posição ainda.</p>}

      {positions.length > 0 && (
        <div className="mb-6 overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Ticker</th>
                <th className="px-4 py-2">Classe</th>
                <th className="px-4 py-2">Qtd.</th>
                <th className="px-4 py-2">Preço médio</th>
                <th className="px-4 py-2">Preço atual</th>
                <th className="px-4 py-2">Valor atual</th>
                <th className="px-4 py-2">Yield on cost</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {positions.map((position) => (
                <tr key={position.assetId}>
                  <td className="px-4 py-2 font-medium">{position.ticker}</td>
                  <td className="px-4 py-2">{assetClassLabels[position.assetClass]}</td>
                  <td className="px-4 py-2">{position.quantity}</td>
                  <td className="px-4 py-2">{currencyFormatter.format(position.averagePrice)}</td>
                  <td className="px-4 py-2">{currencyFormatter.format(position.currentPrice)}</td>
                  <td className="px-4 py-2">{currencyFormatter.format(position.currentValue)}</td>
                  <td className="px-4 py-2">{percentFormatter.format(position.yieldOnCost)}%</td>
                  <td className="px-4 py-2 text-right">
                    <button
                      type="button"
                      onClick={() => {
                        const asset = assets.find((a) => a.id === position.assetId);
                        if (asset) setOperationsAsset(asset);
                      }}
                      className="text-slate-600 hover:underline dark:text-slate-300"
                    >
                      Operações
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2 className="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">Todos os ativos</h2>
      {assets.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              <tr>
                <th className="px-4 py-2">Ticker</th>
                <th className="px-4 py-2">Nome</th>
                <th className="px-4 py-2">Finalidade</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {assets.map((asset) => (
                <tr key={asset.id} className={asset.archived ? "opacity-60" : undefined}>
                  <td className="px-4 py-2 font-medium">{asset.ticker}</td>
                  <td className="px-4 py-2">{asset.name}</td>
                  <td className="px-4 py-2">{investmentPurposeLabels[asset.purpose]}</td>
                  <td className="px-4 py-2">{asset.archived ? "Arquivado" : "Ativo"}</td>
                  <td className="px-4 py-2 text-right">
                    <div className="flex justify-end gap-3">
                      <button
                        type="button"
                        onClick={() => setOperationsAsset(asset)}
                        className="text-slate-600 hover:underline dark:text-slate-300"
                      >
                        Operações
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          const value = window.prompt(`Novo preço atual de ${asset.ticker}:`, String(asset.currentPrice ?? ""));
                          if (value) priceMutation.mutate({ id: asset.id, price: Number(value) });
                        }}
                        className="text-slate-600 hover:underline dark:text-slate-300"
                      >
                        Atualizar preço
                      </button>
                      {!asset.archived && (
                        <button
                          type="button"
                          onClick={() => archiveMutation.mutate(asset.id)}
                          className="text-red-600 hover:underline"
                        >
                          Arquivar
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isCreatingAsset && (
        <Modal title="Novo ativo" onClose={() => setIsCreatingAsset(false)}>
          <form
            onSubmit={handleSubmit((data) => createAssetMutation.mutate(data))}
            className="space-y-4"
            noValidate
          >
            <FormField label="Ticker" htmlFor="asset-ticker" error={errors.ticker?.message}>
              <input id="asset-ticker" className={inputClass} {...register("ticker")} />
            </FormField>
            <FormField label="Nome" htmlFor="asset-name" error={errors.name?.message}>
              <input id="asset-name" className={inputClass} {...register("name")} />
            </FormField>
            <FormField label="Classe" htmlFor="asset-class" error={errors.assetClass?.message}>
              <select id="asset-class" className={inputClass} {...register("assetClass")}>
                {Object.entries(assetClassLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label="Finalidade" htmlFor="asset-purpose" error={errors.purpose?.message}>
              <select id="asset-purpose" className={inputClass} {...register("purpose")}>
                {Object.entries(investmentPurposeLabels).map(([value, label]) => (
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
      )}

      {operationsAsset && (
        <OperationsModal asset={operationsAsset} onClose={() => setOperationsAsset(null)} />
      )}
    </div>
  );
}

const transactionSchema = z.object({
  type: z.enum(["BUY", "SELL", "DIVIDEND", "JCP", "INTEREST", "FEE"]),
  date: z.string().min(1, "Informe a data."),
  quantity: z.coerce.number().min(0, "Não pode ser negativa.").optional(),
  unitPrice: z.coerce.number().min(0, "Não pode ser negativo.").optional(),
  fees: z.coerce.number().min(0, "Não pode ser negativa.").optional(),
  amount: z.coerce.number({ invalid_type_error: "Informe um valor válido." }).min(0, "Não pode ser negativo."),
});
type TransactionFormData = z.infer<typeof transactionSchema>;

function OperationsModal({ asset, onClose }: { asset: InvestmentAsset; onClose: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const transactionsQuery = useQuery({
    queryKey: ["investment-transactions", asset.id],
    queryFn: () => fetchInvestmentTransactions(asset.id),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TransactionFormData>({
    resolver: zodResolver(transactionSchema),
    defaultValues: { type: "BUY", date: new Date().toISOString().slice(0, 10), quantity: 0, unitPrice: 0, fees: 0, amount: 0 },
  });

  const createMutation = useMutation({
    mutationFn: (data: TransactionFormData) =>
      createInvestmentTransaction(asset.id, {
        type: data.type,
        date: data.date,
        quantity: data.quantity ?? 0,
        unitPrice: data.unitPrice ?? 0,
        fees: data.fees ?? 0,
        amount: data.amount,
        accountId: null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["investment-transactions", asset.id] });
      queryClient.invalidateQueries({ queryKey: ["portfolio"] });
      queryClient.invalidateQueries({ queryKey: ["net-worth"] });
      toast.showSuccess("Operação registrada.");
      reset({ type: "BUY", date: new Date().toISOString().slice(0, 10), quantity: 0, unitPrice: 0, fees: 0, amount: 0 });
    },
    onError: (error) => toast.showError(error instanceof ApiError ? error.message : "Erro ao registrar operação."),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteInvestmentTransaction(asset.id, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["investment-transactions", asset.id] });
      queryClient.invalidateQueries({ queryKey: ["portfolio"] });
      toast.showSuccess("Operação removida.");
    },
    onError: () => toast.showError("Erro ao remover operação."),
  });

  return (
    <Modal title={`Operações — ${asset.ticker}`} onClose={onClose}>
      <div className="space-y-4">
        {transactionsQuery.data && transactionsQuery.data.length > 0 && (
          <ul className="max-h-48 divide-y divide-slate-100 overflow-y-auto rounded-md border border-slate-200 dark:divide-slate-800 dark:border-slate-800">
            {transactionsQuery.data.map((tx) => (
              <li key={tx.id} className="flex items-center justify-between px-3 py-2 text-sm">
                <span>
                  {tx.date} · {transactionTypeLabels[tx.type]}
                  {tx.quantity > 0 && ` · ${tx.quantity} @ ${currencyFormatter.format(tx.unitPrice)}`}
                </span>
                <span className="flex items-center gap-3">
                  {currencyFormatter.format(tx.amount)}
                  <button
                    type="button"
                    onClick={() => deleteMutation.mutate(tx.id)}
                    className="text-red-600 hover:underline"
                  >
                    Remover
                  </button>
                </span>
              </li>
            ))}
          </ul>
        )}

        <form
          onSubmit={handleSubmit((data) => createMutation.mutate(data))}
          className="space-y-3 border-t border-slate-200 pt-4 dark:border-slate-800"
          noValidate
        >
          <FormField label="Tipo" htmlFor="tx-type" error={errors.type?.message}>
            <select id="tx-type" className={inputClass} {...register("type")}>
              {Object.entries(transactionTypeLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </FormField>
          <div className="grid grid-cols-2 gap-3">
            <FormField label="Data" htmlFor="tx-date" error={errors.date?.message}>
              <input id="tx-date" type="date" className={inputClass} {...register("date")} />
            </FormField>
            <FormField label="Valor total (R$)" htmlFor="tx-amount" error={errors.amount?.message}>
              <input id="tx-amount" type="number" step="0.01" className={inputClass} {...register("amount")} />
            </FormField>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <FormField label="Quantidade" htmlFor="tx-qty" error={errors.quantity?.message}>
              <input id="tx-qty" type="number" step="0.00000001" className={inputClass} {...register("quantity")} />
            </FormField>
            <FormField label="Preço unit. (R$)" htmlFor="tx-price" error={errors.unitPrice?.message}>
              <input id="tx-price" type="number" step="0.01" className={inputClass} {...register("unitPrice")} />
            </FormField>
            <FormField label="Taxas (R$)" htmlFor="tx-fees" error={errors.fees?.message}>
              <input id="tx-fees" type="number" step="0.01" className={inputClass} {...register("fees")} />
            </FormField>
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
          >
            Adicionar
          </button>
        </form>
      </div>
    </Modal>
  );
}
