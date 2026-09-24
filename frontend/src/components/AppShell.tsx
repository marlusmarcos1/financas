import { Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/AuthProvider";

export function AppShell() {
  const { user, logout } = useAuth();

  return (
    <div className="flex min-h-screen flex-col bg-slate-50 dark:bg-slate-950">
      <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3 dark:border-slate-800 dark:bg-slate-900">
        <span className="font-semibold text-slate-900 dark:text-slate-50">
          Finanças do Marlus
        </span>
        <div className="flex items-center gap-4 text-sm text-slate-600 dark:text-slate-300">
          <span>{user?.displayName}</span>
          <button
            type="button"
            onClick={() => void logout()}
            className="rounded-md border border-slate-300 px-3 py-1.5 hover:bg-slate-100 dark:border-slate-700 dark:hover:bg-slate-800"
          >
            Sair
          </button>
        </div>
      </header>
      <main className="flex-1 px-6 py-6">
        <Outlet />
      </main>
    </div>
  );
}
