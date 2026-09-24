import { api } from "@/lib/api";
import type { PasswordChangeInput, Settings, SettingsInput } from "./types";

export function fetchSettings(): Promise<Settings> {
  return api.get<Settings>("/settings");
}

export function updateSettings(input: SettingsInput): Promise<Settings> {
  return api.put<Settings>("/settings", input);
}

export function changePassword(input: PasswordChangeInput): Promise<void> {
  return api.put<void>("/auth/password", input);
}
