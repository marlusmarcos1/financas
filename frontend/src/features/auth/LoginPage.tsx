import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Navigate, useLocation } from "react-router-dom";
import { z } from "zod";
import { useAuth } from "./AuthProvider";

const loginSchema = z.object({
  username: z.string().min(1, "Informe o usuário."),
  password: z.string().min(1, "Informe a senha."),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginPage() {
  const { user, login, loginError } = useAuth();
  const location = useLocation();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({ resolver: zodResolver(loginSchema) });

  if (user) {
    const redirectTo = (location.state as { from?: string } | null)?.from ?? "/";
    return <Navigate to={redirectTo} replace />;
  }

  const onSubmit = async (data: LoginFormData) => {
    try {
      await login(data.username, data.password);
    } catch {
      // erro já exposto via loginError
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 dark:bg-slate-950">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h1 className="mb-1 text-xl font-semibold text-slate-900 dark:text-slate-50">
          Finanças do Marlus
        </h1>
        <p className="mb-6 text-sm text-slate-500 dark:text-slate-400">
          Entre com seu usuário e senha.
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div>
            <label htmlFor="username" className="mb-1 block text-sm font-medium">
              Usuário
            </label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500 dark:border-slate-700 dark:bg-slate-800"
              {...register("username")}
            />
            {errors.username && (
              <p className="mt-1 text-sm text-red-600">{errors.username.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium">
              Senha
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500 dark:border-slate-700 dark:bg-slate-800"
              {...register("password")}
            />
            {errors.password && (
              <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>
            )}
          </div>

          {loginError && (
            <p role="alert" className="text-sm text-red-600">
              {loginError}
            </p>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:opacity-60 dark:bg-slate-100 dark:text-slate-900"
          >
            {isSubmitting ? "Entrando..." : "Entrar"}
          </button>
        </form>
      </div>
    </div>
  );
}
