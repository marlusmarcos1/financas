import { api } from "@/lib/api";
import type { InstallmentPlan, InstallmentPlanInput } from "./types";

export function fetchInstallmentPlans(): Promise<InstallmentPlan[]> {
  return api.get<InstallmentPlan[]>("/installment-plans");
}

export function createInstallmentPlan(input: InstallmentPlanInput): Promise<InstallmentPlan> {
  return api.post<InstallmentPlan>("/installment-plans", input);
}

export function deleteInstallmentPlan(id: string): Promise<void> {
  return api.del<void>(`/installment-plans/${id}`);
}
