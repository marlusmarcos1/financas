export interface CategoryBudgetStatus {
  categoryId: string;
  categoryName: string;
  limitAmount: number;
  spent: number;
  percent: number;
  status: "GREEN" | "YELLOW" | "RED";
}

export interface Dashboard {
  month: string;
  baseIncomeExpected: number;
  baseIncomeReceived: number;
  extrasReceived: number;
  totalExpenses: number;
  titheDue: number;
  titheOutstanding: number;
  surplus: number;
  budgets: CategoryBudgetStatus[];
}

export interface Alert {
  severity: "WARNING" | "DANGER";
  message: string;
}

export interface ChecklistItem {
  label: string;
  targetAmount: number;
  done: boolean;
}

export interface ScholarshipCountdown {
  sourceId: string;
  name: string;
  remainingMonths: number;
  endsOn: string;
}

export interface DashboardExtras {
  payYourselfFirst: ChecklistItem[];
  scholarships: ScholarshipCountdown[];
}
