export type TitheStatus = "PENDING" | "PAID";

export interface TitheLedgerEntry {
  id: string;
  referenceMonth: string;
  baseAmount: number;
  percent: number;
  dueAmount: number;
  paidAmount: number;
  paidOn: string | null;
  status: TitheStatus;
}

export interface TithePaymentInput {
  amount: number;
  paidOn: string;
}
