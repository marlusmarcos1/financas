export type InvoiceStatus = "OPEN" | "CLOSED" | "PAID";

export interface Invoice {
  id: string;
  cardId: string;
  referenceMonth: string;
  closingDate: string;
  dueDate: string;
  status: InvoiceStatus;
  total: number;
  paidAmount: number;
  paidOn: string | null;
  paidFromAccountId: string | null;
}

export interface PayInvoiceInput {
  accountId: string;
  amount: number;
  paidOn: string;
}

export const invoiceStatusLabels: Record<InvoiceStatus, string> = {
  OPEN: "Aberta",
  CLOSED: "Fechada",
  PAID: "Paga",
};
