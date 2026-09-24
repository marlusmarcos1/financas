import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createContext, useContext, type ReactNode } from "react";
import { fetchCurrentUser, login as loginRequest, logout as logoutRequest } from "./api";
import type { AuthenticatedUser } from "./types";
import { ApiError } from "@/lib/api";

interface AuthContextValue {
  user: AuthenticatedUser | undefined;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  loginError: string | null;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();

  const meQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: fetchCurrentUser,
    retry: false,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });

  const loginMutation = useMutation({
    mutationFn: ({ username, password }: { username: string; password: string }) =>
      loginRequest({ username, password }),
    onSuccess: (user) => {
      queryClient.setQueryData(["auth", "me"], user);
    },
  });

  const logoutMutation = useMutation({
    mutationFn: logoutRequest,
    onSuccess: () => {
      queryClient.setQueryData(["auth", "me"], undefined);
      queryClient.clear();
    },
  });

  const value: AuthContextValue = {
    user: meQuery.data,
    isLoading: meQuery.isLoading,
    login: async (username, password) => {
      await loginMutation.mutateAsync({ username, password });
    },
    logout: async () => {
      await logoutMutation.mutateAsync();
    },
    loginError:
      loginMutation.error instanceof ApiError ? loginMutation.error.message : null,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth deve ser usado dentro de um AuthProvider.");
  }
  return context;
}
