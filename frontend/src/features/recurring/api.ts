import { api } from "@/lib/api";
import type { RecurringRule, RecurringRuleInput } from "./types";

export function fetchRecurringRules(): Promise<RecurringRule[]> {
  return api.get<RecurringRule[]>("/recurring-rules");
}

export function createRecurringRule(input: RecurringRuleInput): Promise<RecurringRule> {
  return api.post<RecurringRule>("/recurring-rules", input);
}

export function updateRecurringRule(id: string, input: RecurringRuleInput): Promise<RecurringRule> {
  return api.put<RecurringRule>(`/recurring-rules/${id}`, input);
}

export function deleteRecurringRule(id: string): Promise<void> {
  return api.del<void>(`/recurring-rules/${id}`);
}
