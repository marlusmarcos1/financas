export type InvestmentAssetClass = "FII" | "STOCK" | "ETF" | "TREASURY" | "CDB" | "SAVINGS_BOX" | "CRYPTO" | "OTHER";
export type InvestmentPurpose = "RETIREMENT" | "HOUSE" | "GENERAL";
export type InvestmentTransactionType = "BUY" | "SELL" | "DIVIDEND" | "JCP" | "INTEREST" | "FEE";

export interface InvestmentAsset {
  id: string;
  ticker: string;
  name: string;
  assetClass: InvestmentAssetClass;
  subclass: string | null;
  indexer: string | null;
  maturityDate: string | null;
  currentPrice: number | null;
  priceUpdatedAt: string | null;
  purpose: InvestmentPurpose;
  archived: boolean;
}

export interface InvestmentAssetInput {
  ticker: string;
  name: string;
  assetClass: InvestmentAssetClass;
  subclass: string;
  indexer: string;
  maturityDate: string | null;
  purpose: InvestmentPurpose;
}

export interface InvestmentTransaction {
  id: string;
  assetId: string;
  type: InvestmentTransactionType;
  date: string;
  quantity: number;
  unitPrice: number;
  fees: number;
  amount: number;
  accountId: string | null;
}

export interface InvestmentTransactionInput {
  type: InvestmentTransactionType;
  date: string;
  quantity: number;
  unitPrice: number;
  fees: number;
  amount: number;
  accountId: string | null;
}

export interface AssetPosition {
  assetId: string;
  ticker: string;
  name: string;
  assetClass: InvestmentAssetClass;
  purpose: InvestmentPurpose;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  totalCost: number;
  currentValue: number;
  unrealizedGain: number;
  totalIncome: number;
  yieldOnCost: number;
}

export interface Portfolio {
  positions: AssetPosition[];
  totalCurrentValue: number;
  totalCost: number;
}

export const assetClassLabels: Record<InvestmentAssetClass, string> = {
  FII: "FII",
  STOCK: "Ação",
  ETF: "ETF",
  TREASURY: "Tesouro Direto",
  CDB: "CDB",
  SAVINGS_BOX: "Poupança",
  CRYPTO: "Cripto",
  OTHER: "Outro",
};

export const investmentPurposeLabels: Record<InvestmentPurpose, string> = {
  RETIREMENT: "Aposentadoria",
  HOUSE: "Casa",
  GENERAL: "Geral",
};

export const transactionTypeLabels: Record<InvestmentTransactionType, string> = {
  BUY: "Compra",
  SELL: "Venda",
  DIVIDEND: "Dividendo",
  JCP: "JCP",
  INTEREST: "Juros",
  FEE: "Taxa",
};
