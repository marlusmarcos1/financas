import { api } from "@/lib/api";
import type { Account, AccountInput } from "./types";

export function fetchAccounts(): Promise<Account[]> {
  return api.get<Account[]>("/accounts");
}

export function createAccount(input: AccountInput): Promise<Account> {
  return api.post<Account>("/accounts", input);
}

export function updateAccount(id: string, input: AccountInput): Promise<Account> {
  return api.put<Account>(`/accounts/${id}`, input);
}

export function archiveAccount(id: string): Promise<void> {
  return api.del<void>(`/accounts/${id}`);
}

export function restoreAccount(id: string): Promise<void> {
  return api.post<void>(`/accounts/${id}/restore`);
}
