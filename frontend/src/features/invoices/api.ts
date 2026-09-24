import { api } from "@/lib/api";
import type { Invoice, PayInvoiceInput } from "./types";

export function fetchInvoicesByCard(cardId: string): Promise<Invoice[]> {
  return api.get<Invoice[]>(`/credit-cards/${cardId}/invoices`);
}

export function fetchAvailableLimit(cardId: string): Promise<{ availableLimit: number }> {
  return api.get<{ availableLimit: number }>(`/credit-cards/${cardId}/available-limit`);
}

export function payInvoice(invoiceId: string, input: PayInvoiceInput): Promise<Invoice> {
  return api.post<Invoice>(`/invoices/${invoiceId}/pay`, input);
}
