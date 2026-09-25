import { api } from "@/lib/api";
import type { EmergencyReserve, NetWorth, NetWorthPoint } from "./types";

export function fetchNetWorth(): Promise<NetWorth> {
  return api.get<NetWorth>("/net-worth");
}

export function fetchNetWorthHistory(months = 6): Promise<NetWorthPoint[]> {
  return api.get<NetWorthPoint[]>(`/net-worth/history?months=${months}`);
}

export function fetchEmergencyReserve(): Promise<EmergencyReserve> {
  return api.get<EmergencyReserve>("/emergency-reserve");
}
