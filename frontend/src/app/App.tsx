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
import { IncomePage } from "@/features/income/IncomePage";
import { InstallmentsPage } from "@/features/installments/InstallmentsPage";
import { RecurringPage } from "@/features/recurring/RecurringPage";
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
          <Route path="/configuracoes" element={<SettingsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
