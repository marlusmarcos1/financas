export interface Settings {
  tithePercent: number;
  installmentLimitPercent: number;
  emergencyMonthsTarget: number;
  currency: string;
}

export type SettingsInput = Settings;

export interface PasswordChangeInput {
  currentPassword: string;
  newPassword: string;
}
