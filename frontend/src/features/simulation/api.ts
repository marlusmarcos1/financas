import { api } from "@/lib/api";
import type { SimulationRequest, SimulationResponse } from "./types";

export function simulateInstallmentPurchase(input: SimulationRequest): Promise<SimulationResponse> {
  return api.post<SimulationResponse>("/simulations/installment-purchase", input);
}
