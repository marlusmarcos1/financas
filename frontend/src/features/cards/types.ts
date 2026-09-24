export interface CreditCard {
  id: string;
  name: string;
  issuer: string | null;
  creditLimit: number;
  closingDay: number;
  dueDay: number;
  defaultPaymentAccountId: string | null;
  color: string | null;
  archived: boolean;
}

export interface CreditCardInput {
  name: string;
  issuer: string;
  creditLimit: number;
  closingDay: number;
  dueDay: number;
  defaultPaymentAccountId: string | null;
  color: string;
}
