import { api } from "@/lib/api";
import type { Goal, GoalContribution, GoalContributionInput, GoalInput } from "./types";

export function fetchGoals(): Promise<Goal[]> {
  return api.get<Goal[]>("/goals");
}

export function createGoal(input: GoalInput): Promise<Goal> {
  return api.post<Goal>("/goals", input);
}

export function updateGoal(id: string, input: GoalInput): Promise<Goal> {
  return api.put<Goal>(`/goals/${id}`, input);
}

export function deleteGoal(id: string): Promise<void> {
  return api.del<void>(`/goals/${id}`);
}

export function fetchGoalContributions(goalId: string): Promise<GoalContribution[]> {
  return api.get<GoalContribution[]>(`/goals/${goalId}/contributions`);
}

export function createGoalContribution(goalId: string, input: GoalContributionInput): Promise<GoalContribution> {
  return api.post<GoalContribution>(`/goals/${goalId}/contributions`, input);
}

export function deleteGoalContribution(goalId: string, id: string): Promise<void> {
  return api.del<void>(`/goals/${goalId}/contributions/${id}`);
}
