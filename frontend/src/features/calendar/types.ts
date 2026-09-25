export interface CalendarItem {
  date: string;
  type: "INVOICE" | "INSTALLMENT" | "RECURRING";
  description: string;
  amount: number;
}
