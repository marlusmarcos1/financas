export interface AccountBalance {
  accountId: string;
  name: string;
  purpose: string;
  balance: number;
}

export interface NetWorth {
  accountsTotal: number;
  investmentsTotal: number;
  netWorth: number;
  accounts: AccountBalance[];
}

export interface EmergencyReserve {
  currentReserve: number;
  averageEssentialMonthlyExpense: number;
  emergencyMonthsTarget: number;
  targetAmount: number;
  monthsOfCoverage: number;
}
