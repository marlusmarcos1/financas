import { api, ApiError } from "@/lib/api";
import type { RetirementPlan, RetirementPlanInput, RetirementProjection } from "./types";

export async function fetchRetirementPlan(): Promise<RetirementPlan | null> {
  try {
    return await api.get<RetirementPlan>("/retirement-plan");
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

export function saveRetirementPlan(input: RetirementPlanInput): Promise<RetirementPlan> {
  return api.put<RetirementPlan>("/retirement-plan", input);
}

export async function fetchRetirementProjection(): Promise<RetirementProjection | null> {
  try {
    return await api.get<RetirementProjection>("/retirement-plan/projection");
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}
