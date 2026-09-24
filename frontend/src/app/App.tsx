import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { AccountsPage } from "@/features/accounts/AccountsPage";
import { AuthProvider } from "@/features/auth/AuthProvider";
import { LoginPage } from "@/features/auth/LoginPage";
import { CardsPage } from "@/features/cards/CardsPage";
import { CategoriesPage } from "@/features/categories/CategoriesPage";
import { DashboardPage } from "@/features/dashboard/DashboardPage";
import { RecurringPage } from "@/features/recurring/RecurringPage";
import { SettingsPage } from "@/features/settings/SettingsPage";
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
          <Route path="/recorrencias" element={<RecurringPage />} />
          <Route path="/configuracoes" element={<SettingsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
