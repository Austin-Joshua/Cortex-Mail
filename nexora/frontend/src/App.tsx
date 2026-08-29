import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { LandingPage }       from './pages/LandingPage';
import { OnboardingPage }    from './pages/OnboardingPage';
import { DashboardPageNew }  from './pages/DashboardPageNew';
import { InboxPage }         from './pages/InboxPage';
import { BrainPage }         from './pages/BrainPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { SettingsPage }      from './pages/SettingsPage';
import { AuthCallbackPage }  from './pages/AuthCallbackPage';
import { EmailDetailPage }   from './pages/EmailDetailPage';
import { AnalyticsPage }     from './pages/AnalyticsPage';
import { ErrorBoundary }     from './components/common/ErrorBoundary';
import { PrivacyPolicyPage } from './pages/PrivacyPolicyPage';
import { PriorityInboxPage } from './pages/PriorityInboxPage';
import { DraftsPage }        from './pages/DraftsPage';
import { ScheduledEmailsPage } from './pages/ScheduledEmailsPage';

import { ArchivePage } from './pages/ArchivePage';
import { SharedPage } from './pages/SharedPage';
import { HelpPage } from './pages/HelpPage';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuthStore();
  return isAuthenticated ? <>{children}</> : <Navigate to="/" replace />;
};

function App() {
  return (
    <ErrorBoundary>
      <Routes>
        {/* Public Routes */}
        <Route path="/"                element={<LandingPage />} />
        <Route path="/auth/callback"   element={<AuthCallbackPage />} />
        <Route path="/privacy"         element={<PrivacyPolicyPage />} />

        {/* Protected Routes */}
        <Route path="/onboarding"      element={<ProtectedRoute><OnboardingPage /></ProtectedRoute>} />

        {/* Main Navigation */}
        <Route path="/dashboard"       element={<ProtectedRoute><DashboardPageNew /></ProtectedRoute>} />
        <Route path="/inbox"           element={<ProtectedRoute><InboxPage /></ProtectedRoute>} />
        <Route path="/priority"        element={<ProtectedRoute><PriorityInboxPage /></ProtectedRoute>} />
        <Route path="/scheduled"       element={<ProtectedRoute><ScheduledEmailsPage /></ProtectedRoute>} />

        {/* Features */}
        <Route path="/brain"           element={<ProtectedRoute><BrainPage /></ProtectedRoute>} />
        <Route path="/drafts"          element={<ProtectedRoute><DraftsPage /></ProtectedRoute>} />
        <Route path="/archive"         element={<ProtectedRoute><ArchivePage /></ProtectedRoute>} />
        <Route path="/shared"          element={<ProtectedRoute><SharedPage /></ProtectedRoute>} />

        {/* Insights */}
        <Route path="/analytics"       element={<ProtectedRoute><AnalyticsPage /></ProtectedRoute>} />
        <Route path="/notifications"   element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />

        {/* System */}
        <Route path="/settings"        element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
        <Route path="/help"            element={<ProtectedRoute><HelpPage /></ProtectedRoute>} />

        {/* Detail Routes */}
        <Route path="/emails/:id"      element={<ProtectedRoute><EmailDetailPage /></ProtectedRoute>} />

        {/* Catch All */}
        <Route path="*"                element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </ErrorBoundary>
  );
}

export default App;
