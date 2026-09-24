import { api } from "@/lib/api";
import type { CreditCard, CreditCardInput } from "./types";

export function fetchCreditCards(): Promise<CreditCard[]> {
  return api.get<CreditCard[]>("/credit-cards");
}

export function createCreditCard(input: CreditCardInput): Promise<CreditCard> {
  return api.post<CreditCard>("/credit-cards", input);
}

export function updateCreditCard(id: string, input: CreditCardInput): Promise<CreditCard> {
  return api.put<CreditCard>(`/credit-cards/${id}`, input);
}

export function archiveCreditCard(id: string): Promise<void> {
  return api.del<void>(`/credit-cards/${id}`);
}

export function restoreCreditCard(id: string): Promise<void> {
  return api.post<void>(`/credit-cards/${id}/restore`);
}
