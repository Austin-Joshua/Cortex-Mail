import type { UserRole } from '../types/User';

export interface AccountRoleOption {
  role: UserRole;
  label: string;
  description: string;
}

export const ACCOUNT_ROLES: AccountRoleOption[] = [
  { role: 'STUDENT', label: 'Student', description: 'Assignments, deadlines, placements & hackathons' },
  { role: 'PROFESSOR', label: 'Professor', description: 'Research, student work & faculty meetings' },
  { role: 'IT_EMPLOYEE', label: 'IT Employee', description: 'System alerts, incidents & tech comms' },
  { role: 'HR_PROFESSIONAL', label: 'HR Professional', description: 'Recruiting, onboarding & policy mail' },
  { role: 'MANAGER', label: 'Manager', description: 'Team updates, approvals & project tracking' },
  { role: 'FREELANCER', label: 'Freelancer', description: 'Client mail, invoices & proposals' },
];

export function accountRoleLabel(role: UserRole | undefined): string {
  return ACCOUNT_ROLES.find((r) => r.role === role)?.label ?? 'Student';
}
