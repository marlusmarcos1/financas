export type GoalType = "HOUSE" | "CAR" | "EMERGENCY" | "OTHER";
export type GoalContributionSource = "SALARY" | "SCHOLARSHIP" | "THIRTEENTH" | "EXTRA" | "MANUAL";

export interface Goal {
  id: string;
  name: string;
  type: GoalType;
  targetAmount: number;
  targetDate: string | null;
  linkedAccountId: string | null;
  monthlyContributionPlanned: number;
  priority: number;
  notes: string | null;
  currentAmount: number;
  amountRemaining: number;
  monthsRemaining: number | null;
  requiredMonthlyContribution: number | null;
  warning: string | null;
}

export interface GoalInput {
  name: string;
  type: GoalType;
  targetAmount: number;
  targetDate: string | null;
  linkedAccountId: string | null;
  monthlyContributionPlanned: number;
  priority: number;
  notes: string;
}

export interface GoalContribution {
  id: string;
  goalId: string;
  date: string;
  amount: number;
  source: GoalContributionSource;
}

export interface GoalContributionInput {
  date: string;
  amount: number;
  source: GoalContributionSource;
}

export const goalTypeLabels: Record<GoalType, string> = {
  HOUSE: "Casa",
  CAR: "Carro",
  EMERGENCY: "Reserva de emergência",
  OTHER: "Outra",
};

export const contributionSourceLabels: Record<GoalContributionSource, string> = {
  SALARY: "Salário",
  SCHOLARSHIP: "Bolsa",
  THIRTEENTH: "13º",
  EXTRA: "Extra",
  MANUAL: "Manual",
};
