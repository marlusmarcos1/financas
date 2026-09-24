import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/AuthProvider";

const navItems = [
  { to: "/", label: "Dashboard", end: true },
  { to: "/contas", label: "Contas" },
  { to: "/cartoes", label: "Cartões" },
  { to: "/categorias", label: "Categorias" },
  { to: "/configuracoes", label: "Configurações" },
];

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
      <div className="flex flex-1">
        <nav className="w-48 shrink-0 border-r border-slate-200 bg-white px-3 py-4 dark:border-slate-800 dark:bg-slate-900">
          <ul className="space-y-1">
            {navItems.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    `block rounded-md px-3 py-2 text-sm font-medium ${
                      isActive
                        ? "bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900"
                        : "text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <main className="flex-1 px-6 py-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
