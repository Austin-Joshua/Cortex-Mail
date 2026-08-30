/** @deprecated Profession presets removed — Cortex Mail classifies per mailbox. */
export type AccountRoleOption = {
  role: string;
  label: string;
  description: string;
};

export const ACCOUNT_ROLES: AccountRoleOption[] = [];

export function getRoleLabel(_role?: string): string {
  return 'Account';
}
