import { api } from "@/lib/api";
import type { Dashboard } from "./types";

export function fetchDashboard(month: string): Promise<Dashboard> {
  return api.get<Dashboard>(`/dashboard?month=${month}`);
}
