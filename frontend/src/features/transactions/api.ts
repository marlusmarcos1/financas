import { api } from "@/lib/api";
import type { Transaction, TransactionInput, TransactionPage } from "./types";

export interface TransactionFilters {
  from?: string;
  to?: string;
  categoryId?: string;
  accountId?: string;
  cardId?: string;
  status?: string;
  kind?: string;
}

export function fetchTransactions(filters: TransactionFilters = {}): Promise<TransactionPage> {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });
  params.set("size", "50");
  params.set("sort", "date,desc");
  const query = params.toString();
  return api.get<TransactionPage>(`/transactions${query ? `?${query}` : ""}`);
}

export function createTransaction(input: TransactionInput): Promise<Transaction> {
  return api.post<Transaction>("/transactions", input);
}

export function updateTransaction(id: string, input: TransactionInput): Promise<Transaction> {
  return api.put<Transaction>(`/transactions/${id}`, input);
}

export function deleteTransaction(id: string): Promise<void> {
  return api.del<void>(`/transactions/${id}`);
}

export function markTransactionAsPaid(id: string): Promise<void> {
  return api.post<void>(`/transactions/${id}/mark-as-paid`);
}
