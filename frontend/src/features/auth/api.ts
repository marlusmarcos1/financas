import { api } from "@/lib/api";
import type { AuthenticatedUser } from "./types";

export interface LoginPayload {
  username: string;
  password: string;
}

export function fetchCurrentUser(): Promise<AuthenticatedUser> {
  return api.get<AuthenticatedUser>("/auth/me");
}

export function login(payload: LoginPayload): Promise<AuthenticatedUser> {
  return api.post<AuthenticatedUser>("/auth/login", payload);
}

export function logout(): Promise<void> {
  return api.post<void>("/auth/logout");
}
