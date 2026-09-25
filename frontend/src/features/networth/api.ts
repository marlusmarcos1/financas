import { api } from "@/lib/api";
import type { EmergencyReserve, NetWorth } from "./types";

export function fetchNetWorth(): Promise<NetWorth> {
  return api.get<NetWorth>("/net-worth");
}

export function fetchEmergencyReserve(): Promise<EmergencyReserve> {
  return api.get<EmergencyReserve>("/emergency-reserve");
}
