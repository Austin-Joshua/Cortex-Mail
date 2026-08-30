type NotificationType = 'DEADLINE' | 'ACTION_REQUIRED' | 'IMPORTANT_EMAIL' | 'DAILY_DIGEST';

export interface Notification {
  id: number;
  userId: number;
  title: string;
  message: string;
  notificationType: NotificationType;
  relatedEmailId?: number;
  isRead: boolean;
  scheduledAt?: string;
  createdAt?: string;
}
