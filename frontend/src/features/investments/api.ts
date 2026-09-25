import { api } from "@/lib/api";
import type {
  InvestmentAsset,
  InvestmentAssetInput,
  InvestmentTransaction,
  InvestmentTransactionInput,
  Portfolio,
} from "./types";

export function fetchInvestmentAssets(): Promise<InvestmentAsset[]> {
  return api.get<InvestmentAsset[]>("/investment-assets");
}

export function createInvestmentAsset(input: InvestmentAssetInput): Promise<InvestmentAsset> {
  return api.post<InvestmentAsset>("/investment-assets", input);
}

export function archiveInvestmentAsset(id: string): Promise<void> {
  return api.del<void>(`/investment-assets/${id}`);
}

export function updateAssetPrice(id: string, price: number): Promise<InvestmentAsset> {
  return api.put<InvestmentAsset>(`/investment-assets/${id}/price`, { price });
}

export function fetchInvestmentTransactions(assetId: string): Promise<InvestmentTransaction[]> {
  return api.get<InvestmentTransaction[]>(`/investment-assets/${assetId}/transactions`);
}

export function createInvestmentTransaction(
  assetId: string,
  input: InvestmentTransactionInput,
): Promise<InvestmentTransaction> {
  return api.post<InvestmentTransaction>(`/investment-assets/${assetId}/transactions`, input);
}

export function deleteInvestmentTransaction(assetId: string, id: string): Promise<void> {
  return api.del<void>(`/investment-assets/${assetId}/transactions/${id}`);
}

export function fetchPortfolio(): Promise<Portfolio> {
  return api.get<Portfolio>("/portfolio");
}
