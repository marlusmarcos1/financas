import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FormField, inputClass } from "@/components/FormField";
import { useToast } from "@/components/ToastProvider";
import { ApiError } from "@/lib/api";
import {
  applyImport,
  downloadExport,
  dryRunImport,
  fetchImportJobs,
  type ImportMode,
  type ImportSummary,
} from "./api";

export function DataIoPage() {
  const toast = useToast();
  const [file, setFile] = useState<File | null>(null);
  const [mode, setMode] = useState<ImportMode>("MERGE");
  const [confirmation, setConfirmation] = useState("");
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const [busy, setBusy] = useState<"none" | "export" | "dry-run" | "apply">("none");

  const jobsQuery = useQuery({ queryKey: ["import-jobs"], queryFn: fetchImportJobs });

  async function handleExport() {
    setBusy("export");
    try {
      await downloadExport();
      toast.showSuccess("Exportação gerada.");
    } catch (error) {
      toast.showError(error instanceof ApiError ? error.message : "Erro ao exportar.");
    } finally {
      setBusy("none");
    }
  }

  async function handleDryRun() {
    if (!file) {
      toast.showError("Selecione um arquivo .zip exportado pelo app.");
      return;
    }
    setBusy("dry-run");
    try {
      const result = await dryRunImport(file);
      setSummary(result);
      toast.showSuccess(
        result.totalErrors === 0
          ? "Arquivo válido — pronto para aplicar."
          : `${result.totalErrors} erro(s) encontrado(s). Corrija antes de aplicar.`,
      );
    } catch (error) {
      toast.showError(error instanceof ApiError ? error.message : "Erro ao validar o arquivo.");
    } finally {
      setBusy("none");
    }
  }

  async function handleApply() {
    if (!file) {
      toast.showError("Selecione um arquivo .zip exportado pelo app.");
      return;
    }
    if (mode === "REPLACE" && confirmation !== "SUBSTITUIR") {
      toast.showError('Digite exatamente "SUBSTITUIR" para confirmar a substituição total.');
      return;
    }
    setBusy("apply");
    try {
      const result = await applyImport(file, mode, mode === "REPLACE" ? confirmation : undefined);
      setSummary(result);
      if (result.totalErrors > 0) {
        toast.showError(`Importação não aplicada: ${result.totalErrors} erro(s).`);
      } else {
        toast.showSuccess(
          `Importação aplicada: ${result.totalCreated} criado(s), ${result.totalUpdated} atualizado(s).`,
        );
        setConfirmation("");
        void jobsQuery.refetch();
      }
    } catch (error) {
      toast.showError(error instanceof ApiError ? error.message : "Erro ao aplicar a importação.");
    } finally {
      setBusy("none");
    }
  }

  return (
    <div className="max-w-3xl space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Exportar / Importar</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          Faça backup dos seus dados em CSV/ZIP ou restaure a partir de um arquivo exportado anteriormente.
        </p>
      </div>

      <section className="rounded-xl border border-slate-200 p-6 dark:border-slate-800">
        <h2 className="mb-4 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">Exportar</h2>
        <p className="mb-4 text-sm text-slate-600 dark:text-slate-300">
          Gera um arquivo .zip com um CSV por tipo de dado e um manifesto com checksums.
        </p>
        <button
          type="button"
          onClick={() => void handleExport()}
          disabled={busy === "export"}
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50 dark:bg-slate-100 dark:text-slate-900"
        >
          {busy === "export" ? "Exportando..." : "Exportar tudo (.zip)"}
        </button>
      </section>

      <section className="rounded-xl border border-slate-200 p-6 dark:border-slate-800">
        <h2 className="mb-4 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">Importar</h2>

        <FormField label="Arquivo (.zip)" htmlFor="import-file">
          <input
            id="import-file"
            type="file"
            accept=".zip"
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null);
              setSummary(null);
            }}
            className="block w-full text-sm text-slate-600 file:mr-4 file:rounded-md file:border-0 file:bg-slate-100 file:px-3 file:py-2 file:text-sm file:font-medium hover:file:bg-slate-200 dark:text-slate-300 dark:file:bg-slate-800"
          />
        </FormField>

        <div className="mt-4">
          <FormField label="Modo" htmlFor="import-mode">
            <select
              id="import-mode"
              className={inputClass}
              value={mode}
              onChange={(event) => setMode(event.target.value as ImportMode)}
            >
              <option value="MERGE">Mesclar (atualiza por id, mantém o resto)</option>
              <option value="REPLACE">Substituir tudo (apaga os dados atuais antes de importar)</option>
            </select>
          </FormField>
        </div>

        {mode === "REPLACE" && (
          <div className="mt-4">
            <FormField
              label='Digite "SUBSTITUIR" para confirmar'
              htmlFor="import-confirmation"
            >
              <input
                id="import-confirmation"
                type="text"
                className={inputClass}
                value={confirmation}
                onChange={(event) => setConfirmation(event.target.value)}
                placeholder="SUBSTITUIR"
              />
            </FormField>
            <p className="mt-1 text-sm text-red-600">
              Atenção: isso apaga permanentemente todos os seus dados atuais antes de importar.
            </p>
          </div>
        )}

        <div className="mt-4 flex gap-3">
          <button
            type="button"
            onClick={() => void handleDryRun()}
            disabled={busy !== "none" || !file}
            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
          >
            {busy === "dry-run" ? "Validando..." : "Validar (sem aplicar)"}
          </button>
          <button
            type="button"
            onClick={() => void handleApply()}
            disabled={busy !== "none" || !file}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50 dark:bg-slate-100 dark:text-slate-900"
          >
            {busy === "apply" ? "Aplicando..." : "Aplicar importação"}
          </button>
        </div>

        {summary && <ImportSummaryView summary={summary} />}
      </section>

      <section className="rounded-xl border border-slate-200 p-6 dark:border-slate-800">
        <h2 className="mb-4 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
          Histórico de importações
        </h2>
        {jobsQuery.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
        {jobsQuery.data && jobsQuery.data.length === 0 && (
          <p className="text-sm text-slate-500">Nenhuma importação registrada ainda.</p>
        )}
        {jobsQuery.data && jobsQuery.data.length > 0 && (
          <ul className="divide-y divide-slate-200 text-sm dark:divide-slate-800">
            {jobsQuery.data.map((job) => (
              <li key={job.id} className="flex items-center justify-between py-2">
                <div>
                  <p className="font-medium text-slate-800 dark:text-slate-100">{job.filename}</p>
                  <p className="text-slate-500 dark:text-slate-400">
                    {new Date(job.createdAt).toLocaleString("pt-BR")} · {job.mode}
                  </p>
                </div>
                <StatusBadge status={job.status} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const label = { DRY_RUN: "Validado", APPLIED: "Aplicado", FAILED: "Falhou" }[status] ?? status;
  const className =
    status === "APPLIED"
      ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900 dark:text-emerald-200"
      : status === "FAILED"
        ? "bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-200"
        : "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200";
  return <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${className}`}>{label}</span>;
}

function ImportSummaryView({ summary }: { summary: ImportSummary }) {
  return (
    <div className="mt-6 space-y-3">
      <div className="flex gap-4 text-sm">
        <span className="text-emerald-700 dark:text-emerald-400">{summary.totalCreated} criado(s)</span>
        <span className="text-sky-700 dark:text-sky-400">{summary.totalUpdated} atualizado(s)</span>
        <span className="text-red-700 dark:text-red-400">{summary.totalErrors} erro(s)</span>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="text-slate-500 dark:text-slate-400">
            <tr>
              <th className="py-1 pr-4">Arquivo</th>
              <th className="py-1 pr-4">Criados</th>
              <th className="py-1 pr-4">Atualizados</th>
              <th className="py-1 pr-4">Erros</th>
            </tr>
          </thead>
          <tbody>
            {summary.files
              .filter((f) => f.created + f.updated + f.errors > 0)
              .map((f) => (
                <tr key={f.file} className="border-t border-slate-100 dark:border-slate-800">
                  <td className="py-1 pr-4">{f.file}</td>
                  <td className="py-1 pr-4">{f.created}</td>
                  <td className="py-1 pr-4">{f.updated}</td>
                  <td className="py-1 pr-4">{f.errors}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
      {summary.errors.length > 0 && (
        <div>
          <p className="mb-1 text-sm font-medium text-red-700 dark:text-red-400">Erros:</p>
          <ul className="max-h-64 space-y-1 overflow-y-auto text-sm text-red-600 dark:text-red-300">
            {summary.errors.map((error, index) => (
              <li key={`${error.file}-${error.line}-${index}`}>
                {error.file} (linha {error.line}): {error.message}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
