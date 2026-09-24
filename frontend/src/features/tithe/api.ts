import { api } from "@/lib/api";
import type { TitheLedgerEntry, TithePaymentInput } from "./types";

export function fetchTitheLedger(): Promise<TitheLedgerEntry[]> {
  return api.get<TitheLedgerEntry[]>("/tithe-ledger");
}

export function payTithe(id: string, input: TithePaymentInput): Promise<TitheLedgerEntry> {
  return api.post<TitheLedgerEntry>(`/tithe-ledger/${id}/pay`, input);
}
