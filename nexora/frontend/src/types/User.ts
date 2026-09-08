export type UserRole = 'STUDENT' | 'PROFESSOR' | 'IT_EMPLOYEE' | 'HR_PROFESSIONAL' | 'MANAGER' | 'FREELANCER';

export interface User {
  userId: number;
  email: string;
  name: string;
  profilePictureUrl?: string;
  userRole: UserRole;
  onboardingComplete: boolean;
  calendarSyncEnabled?: boolean;
  lastSyncedAt?: string;
  quietHoursStart?: number | null;
  quietHoursEnd?: number | null;
  mutedCategories?: string | null;
  digestEnabled?: boolean | null;
  digestHour?: number | null;
}

export interface AuthResponse {
  token?: string | null;
  tokenType: string;
  userId: number;
  email: string;
  name: string;
  profilePictureUrl?: string;
  userRole: UserRole;
  onboardingComplete: boolean;
  calendarSyncEnabled?: boolean;
  lastSyncedAt?: string;
  quietHoursStart?: number | null;
  quietHoursEnd?: number | null;
  mutedCategories?: string | null;
  digestEnabled?: boolean | null;
  digestHour?: number | null;
}
