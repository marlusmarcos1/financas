import { api } from "@/lib/api";
import type { CalendarItem } from "./types";

export function fetchCalendar(from: string, to: string): Promise<CalendarItem[]> {
  return api.get<CalendarItem[]>(`/calendar?from=${from}&to=${to}`);
}
