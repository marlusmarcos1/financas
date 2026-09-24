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
