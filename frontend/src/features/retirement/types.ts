export interface RetirementPlan {
  id: string;
  monthlyContribution: number;
  contributionAnnualIncreasePercent: number;
  startDate: string;
  horizonYears: number;
  expectedReturnNominalAnnual: number;
  expectedInflationAnnual: number;
  currentBalance: number;
}

export interface RetirementPlanInput {
  monthlyContribution: number;
  contributionAnnualIncreasePercent: number;
  startDate: string;
  horizonYears: number;
  expectedReturnNominalAnnual: number;
  expectedInflationAnnual: number;
  currentBalance: number;
}

export interface YearSnapshot {
  year: number;
  balance: number;
}

export interface RetirementScenario {
  label: string;
  annualRate: number;
  finalBalanceNominal: number;
  totalContributed: number;
  interestEarned: number;
  finalBalanceReal: number;
  yearlyBalances: YearSnapshot[];
}

export interface RetirementProjection {
  scenarios: RetirementScenario[];
}
