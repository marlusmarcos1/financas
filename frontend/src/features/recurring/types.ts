export type RecurringFrequency = "MONTHLY" | "YEARLY";

export interface RecurringRule {
  id: string;
  description: string;
  amount: number;
  amountIsVariable: boolean;
  frequency: RecurringFrequency;
  dayOfMonth: number;
  startDate: string;
  endDate: string | null;
  categoryId: string;
  cardId: string | null;
  accountId: string | null;
  active: boolean;
}

export interface RecurringRuleInput {
  description: string;
  amount: number;
  amountIsVariable: boolean;
  frequency: RecurringFrequency;
  dayOfMonth: number;
  startDate: string;
  endDate: string | null;
  categoryId: string;
  cardId: string | null;
  accountId: string | null;
  active: boolean;
}

export const recurringFrequencyLabels: Record<RecurringFrequency, string> = {
  MONTHLY: "Mensal",
  YEARLY: "Anual",
};
