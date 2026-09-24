export type AccountType = "CHECKING" | "SAVINGS_BOX" | "BROKERAGE" | "CASH";
export type AccountPurpose = "DAILY" | "EMERGENCY_RESERVE" | "GOAL" | "INVESTMENT";

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  institution: string | null;
  initialBalance: number;
  purpose: AccountPurpose;
  archived: boolean;
}

export interface AccountInput {
  name: string;
  type: AccountType;
  institution: string;
  initialBalance: number;
  purpose: AccountPurpose;
}

export const accountTypeLabels: Record<AccountType, string> = {
  CHECKING: "Conta corrente",
  SAVINGS_BOX: "Caixinha / poupança",
  BROKERAGE: "Corretora",
  CASH: "Dinheiro",
};

export const accountPurposeLabels: Record<AccountPurpose, string> = {
  DAILY: "Uso diário",
  EMERGENCY_RESERVE: "Reserva de emergência",
  GOAL: "Meta",
  INVESTMENT: "Investimento",
};
