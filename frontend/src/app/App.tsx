import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { AccountsPage } from "@/features/accounts/AccountsPage";
import { AuthProvider } from "@/features/auth/AuthProvider";
import { LoginPage } from "@/features/auth/LoginPage";
import { CardsPage } from "@/features/cards/CardsPage";
import { CategoriesPage } from "@/features/categories/CategoriesPage";
import { CommitmentsPage } from "@/features/commitments/CommitmentsPage";
import { DashboardPage } from "@/features/dashboard/DashboardPage";
import { DataIoPage } from "@/features/dataio/DataIoPage";
import { GoalsPage } from "@/features/goals/GoalsPage";
import { IncomePage } from "@/features/income/IncomePage";
import { InstallmentsPage } from "@/features/installments/InstallmentsPage";
import { InvestmentsPage } from "@/features/investments/InvestmentsPage";
import { RecurringPage } from "@/features/recurring/RecurringPage";
import { RetirementPage } from "@/features/retirement/RetirementPage";
import { SettingsPage } from "@/features/settings/SettingsPage";
import { SimulatorPage } from "@/features/simulation/SimulatorPage";
import { TithePage } from "@/features/tithe/TithePage";
import { TransactionsPage } from "@/features/transactions/TransactionsPage";

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <RequireAuth>
              <AppShell />
            </RequireAuth>
          }
        >
          <Route path="/" element={<DashboardPage />} />
          <Route path="/lancamentos" element={<TransactionsPage />} />
          <Route path="/contas" element={<AccountsPage />} />
          <Route path="/cartoes" element={<CardsPage />} />
          <Route path="/categorias" element={<CategoriesPage />} />
          <Route path="/parcelamentos" element={<InstallmentsPage />} />
          <Route path="/simulador" element={<SimulatorPage />} />
          <Route path="/compromissos" element={<CommitmentsPage />} />
          <Route path="/receitas" element={<IncomePage />} />
          <Route path="/dizimo" element={<TithePage />} />
          <Route path="/recorrencias" element={<RecurringPage />} />
          <Route path="/investimentos" element={<InvestmentsPage />} />
          <Route path="/aposentadoria" element={<RetirementPage />} />
          <Route path="/metas" element={<GoalsPage />} />
          <Route path="/configuracoes" element={<SettingsPage />} />
          <Route path="/dados" element={<DataIoPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
