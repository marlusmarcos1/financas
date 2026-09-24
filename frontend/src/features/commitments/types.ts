export interface EndingPlan {
  planId: string;
  description: string;
  monthlyAmount: number;
}

export interface MonthCommitment {
  month: string;
  installmentsTotal: number;
  recurringTotal: number;
  committedTotal: number;
  endingPlans: EndingPlan[];
}
