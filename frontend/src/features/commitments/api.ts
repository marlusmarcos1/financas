import { api } from "@/lib/api";
import type { MonthCommitment } from "./types";

export function fetchCommitments(months: number): Promise<MonthCommitment[]> {
  return api.get<MonthCommitment[]>(`/commitments?months=${months}`);
}
