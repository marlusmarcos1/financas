export type TransactionKind = "INCOME" | "EXPENSE" | "TRANSFER";
export type TransactionStatus = "PLANNED" | "PAID";

export interface Transaction {
  id: string;
  kind: TransactionKind;
  description: string;
  amount: number;
  date: string;
  categoryId: string | null;
  accountId: string | null;
  cardId: string | null;
  invoiceId: string | null;
  status: TransactionStatus;
  installmentPlanId: string | null;
  installmentNumber: number | null;
  recurringRuleId: string | null;
  notes: string | null;
}

export interface TransactionInput {
  kind: TransactionKind;
  description: string;
  amount: number;
  date: string;
  categoryId: string | null;
  accountId: string | null;
  cardId: string | null;
  status: TransactionStatus;
  notes: string;
}

export interface TransactionPage {
  content: Transaction[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const transactionKindLabels: Record<TransactionKind, string> = {
  INCOME: "Receita",
  EXPENSE: "Despesa",
  TRANSFER: "Transferência",
};

export const transactionStatusLabels: Record<TransactionStatus, string> = {
  PLANNED: "Planejado",
  PAID: "Pago",
};
