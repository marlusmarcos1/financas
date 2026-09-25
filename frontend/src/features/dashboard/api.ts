import { api } from "@/lib/api";
import type { Alert, Dashboard, DashboardExtras } from "./types";

export function fetchDashboard(month: string): Promise<Dashboard> {
  return api.get<Dashboard>(`/dashboard?month=${month}`);
}

export function fetchAlerts(): Promise<Alert[]> {
  return api.get<Alert[]>("/dashboard/alerts");
}

export function fetchDashboardExtras(month: string): Promise<DashboardExtras> {
  return api.get<DashboardExtras>(`/dashboard/extras?month=${month}`);
}
