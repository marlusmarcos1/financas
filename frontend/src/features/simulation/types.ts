export type SimulationVerdict = "SAFE" | "WARNING" | "NOT_RECOMMENDED";

export interface SimulationRequest {
  description: string;
  totalAmount: number;
  installmentCount: number;
  cardId: string;
  purchaseDate: string;
  interestRateMonthly?: number;
}

export interface MonthSimulation {
  month: string;
  existingInstallments: number;
  newInstallment: number;
  fixedRecurring: number;
  averageVariableSpend: number;
  titheDue: number;
  totalCommittedInstallments: number;
  commitmentPercent: number;
  freeBalance: number;
  freeBalanceWithoutPurchase: number;
}

export interface CardLimitImpact {
  creditLimit: number;
  availableLimitBefore: number;
  availableLimitAfter: number;
}

export interface SimulationResponse {
  verdict: SimulationVerdict;
  installmentAmount: number;
  maxSafeInstallmentToday: number;
  cardLimitImpact: CardLimitImpact;
  months: MonthSimulation[];
  suggestions: string[];
}

export const verdictLabels: Record<SimulationVerdict, string> = {
  SAFE: "Seguro",
  WARNING: "Atenção",
  NOT_RECOMMENDED: "Não recomendado",
};

export const verdictColors: Record<SimulationVerdict, string> = {
  SAFE: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200",
  WARNING: "bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200",
  NOT_RECOMMENDED: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200",
};
