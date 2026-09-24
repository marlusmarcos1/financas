import { api } from "@/lib/api";
import type { IncomeEntry, IncomeEntryInput, IncomeSource, IncomeSourceInput } from "./types";

export function fetchIncomeSources(): Promise<IncomeSource[]> {
  return api.get<IncomeSource[]>("/income-sources");
}

export function createIncomeSource(input: IncomeSourceInput): Promise<IncomeSource> {
  return api.post<IncomeSource>("/income-sources", input);
}

export function deleteIncomeSource(id: string): Promise<void> {
  return api.del<void>(`/income-sources/${id}`);
}

export function fetchIncomeEntries(referenceMonth: string): Promise<IncomeEntry[]> {
  return api.get<IncomeEntry[]>(`/income-entries?referenceMonth=${referenceMonth}`);
}

export function createIncomeEntry(input: IncomeEntryInput): Promise<IncomeEntry> {
  return api.post<IncomeEntry>("/income-entries", input);
}

export function deleteIncomeEntry(id: string): Promise<void> {
  return api.del<void>(`/income-entries/${id}`);
}
