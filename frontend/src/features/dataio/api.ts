import { API_BASE, ApiError, readCookie } from "@/lib/api";

export type ImportMode = "MERGE" | "REPLACE";
export type ImportJobStatus = "DRY_RUN" | "APPLIED" | "FAILED";

export interface FileSummary {
  file: string;
  created: number;
  updated: number;
  errors: number;
}

export interface RowError {
  file: string;
  line: number;
  message: string;
}

export interface ImportSummary {
  applied: boolean;
  mode: ImportMode;
  files: FileSummary[];
  errors: RowError[];
  totalCreated: number;
  totalUpdated: number;
  totalErrors: number;
}

export interface ImportJob {
  id: string;
  filename: string;
  mode: ImportMode;
  status: ImportJobStatus;
  summaryJson: string;
  createdAt: string;
}

async function ensureCsrfToken(): Promise<string | null> {
  let token = readCookie("XSRF-TOKEN");
  if (!token) {
    await fetch(`${API_BASE}/auth/csrf`, { credentials: "include" });
    token = readCookie("XSRF-TOKEN");
  }
  return token;
}

export async function downloadExport(): Promise<void> {
  const response = await fetch(`${API_BASE}/data/export`, { credentials: "include" });
  if (!response.ok) {
    throw new ApiError(response.status, "Erro", "Não foi possível exportar os dados.");
  }
  const blob = await response.blob();
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = disposition.match(/filename="([^"]+)"/);
  const filename = match?.[1] ?? "financas-export.zip";

  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

async function uploadZip(path: string, file: File, params: Record<string, string> = {}): Promise<ImportSummary> {
  const token = await ensureCsrfToken();
  const formData = new FormData();
  formData.append("file", file);

  const search = new URLSearchParams(params).toString();
  const response = await fetch(`${API_BASE}${path}${search ? `?${search}` : ""}`, {
    method: "POST",
    credentials: "include",
    headers: token ? { "X-XSRF-TOKEN": token } : undefined,
    body: formData,
  });

  const isJson = response.headers.get("content-type")?.includes("json");
  const body = isJson ? await response.json() : undefined;

  if (!response.ok) {
    throw new ApiError(response.status, body?.title ?? "Erro", body?.detail ?? "Erro ao processar o arquivo.", body?.errors);
  }
  return body as ImportSummary;
}

export function dryRunImport(file: File): Promise<ImportSummary> {
  return uploadZip("/data/import/dry-run", file);
}

export function applyImport(file: File, mode: ImportMode, confirmation?: string): Promise<ImportSummary> {
  const params: Record<string, string> = { mode };
  if (confirmation) {
    params.confirmation = confirmation;
  }
  return uploadZip("/data/import/apply", file, params);
}

export async function fetchImportJobs(): Promise<ImportJob[]> {
  const response = await fetch(`${API_BASE}/data/import/jobs`, { credentials: "include" });
  if (!response.ok) {
    throw new ApiError(response.status, "Erro", "Não foi possível carregar o histórico de importações.");
  }
  return response.json();
}
