export type IncomeSourceType = "SALARY" | "SCHOLARSHIP" | "THIRTEENTH" | "EXTRA" | "OTHER";
export type IncomeRecurrence = "MONTHLY" | "TEMPORARY" | "SPORADIC";
export type IncomeEntryStatus = "EXPECTED" | "RECEIVED";

export interface IncomeSource {
  id: string;
  name: string;
  type: IncomeSourceType;
  recurrence: IncomeRecurrence;
  expectedAmount: number;
  payDay: number | null;
  startDate: string | null;
  endDate: string | null;
  expectedMonths: number | null;
  titheApplies: boolean;
  countsInBaseBudget: boolean;
}

export interface IncomeSourceInput {
  name: string;
  type: IncomeSourceType;
  recurrence: IncomeRecurrence;
  expectedAmount: number;
  payDay: number | null;
  startDate: string | null;
  endDate: string | null;
  expectedMonths: number | null;
  titheApplies: boolean;
  countsInBaseBudget: boolean;
}

export interface IncomeEntry {
  id: string;
  sourceId: string;
  accountId: string | null;
  referenceMonth: string;
  receivedOn: string | null;
  amount: number;
  status: IncomeEntryStatus;
}

export interface IncomeEntryInput {
  sourceId: string;
  accountId: string | null;
  referenceMonth: string;
  receivedOn: string | null;
  amount: number;
  status: IncomeEntryStatus;
}

export const incomeSourceTypeLabels: Record<IncomeSourceType, string> = {
  SALARY: "Salário",
  SCHOLARSHIP: "Bolsa",
  THIRTEENTH: "13º salário",
  EXTRA: "Extra",
  OTHER: "Outro",
};

export const incomeRecurrenceLabels: Record<IncomeRecurrence, string> = {
  MONTHLY: "Mensal",
  TEMPORARY: "Temporária",
  SPORADIC: "Esporádica",
};

export const incomeEntryStatusLabels: Record<IncomeEntryStatus, string> = {
  EXPECTED: "Esperado",
  RECEIVED: "Recebido",
};
