export interface InstallmentPlan {
  id: string;
  cardId: string;
  description: string;
  purchaseDate: string;
  totalAmount: number;
  installmentCount: number;
  installmentAmount: number;
  firstInstallmentNumber: number;
  firstInvoiceMonth: string;
  lastInstallmentMonth: string;
  interestRateMonthly: number | null;
  totalToPay: number;
  totalInterest: number;
  effectiveRate: number | null;
  categoryId: string | null;
}

export interface InstallmentPlanInput {
  cardId: string;
  description: string;
  purchaseDate: string;
  totalAmount: number;
  installmentCount: number;
  firstInstallmentNumber?: number;
  firstInvoiceMonth?: string;
  interestRateMonthly?: number;
  categoryId: string | null;
}
