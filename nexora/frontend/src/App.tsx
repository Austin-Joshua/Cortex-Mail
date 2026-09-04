import React, { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { ErrorBoundary }     from './components/common/ErrorBoundary';
import { LoadingSpinner } from './components/common/LoadingSpinner';

const LandingPage = lazy(() => import('./pages/LandingPage').then((m) => ({ default: m.LandingPage })));
const OnboardingPage = lazy(() => import('./pages/OnboardingPage').then((m) => ({ default: m.OnboardingPage })));
const DashboardPageNew = lazy(() => import('./pages/DashboardPageNew').then((m) => ({ default: m.DashboardPageNew })));
const InboxPage = lazy(() => import('./pages/InboxPage').then((m) => ({ default: m.InboxPage })));
const BrainPage = lazy(() => import('./pages/BrainPage').then((m) => ({ default: m.BrainPage })));
const NotificationsPage = lazy(() => import('./pages/NotificationsPage').then((m) => ({ default: m.NotificationsPage })));
const SettingsPage = lazy(() => import('./pages/SettingsPage').then((m) => ({ default: m.SettingsPage })));
const AuthCallbackPage = lazy(() => import('./pages/AuthCallbackPage').then((m) => ({ default: m.AuthCallbackPage })));
const EmailDetailPage = lazy(() => import('./pages/EmailDetailPage').then((m) => ({ default: m.EmailDetailPage })));
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage').then((m) => ({ default: m.AnalyticsPage })));
const PrivacyPolicyPage = lazy(() => import('./pages/PrivacyPolicyPage').then((m) => ({ default: m.PrivacyPolicyPage })));
const PriorityInboxPage = lazy(() => import('./pages/PriorityInboxPage').then((m) => ({ default: m.PriorityInboxPage })));
const DraftsPage = lazy(() => import('./pages/DraftsPage').then((m) => ({ default: m.DraftsPage })));
const ScheduledEmailsPage = lazy(() => import('./pages/ScheduledEmailsPage').then((m) => ({ default: m.ScheduledEmailsPage })));
const ArchivePage = lazy(() => import('./pages/ArchivePage').then((m) => ({ default: m.ArchivePage })));
const SharedPage = lazy(() => import('./pages/SharedPage').then((m) => ({ default: m.SharedPage })));
const HelpPage = lazy(() => import('./pages/HelpPage').then((m) => ({ default: m.HelpPage })));

const ProtectedRoute: React.FC<{ children: React.ReactNode; requireOnboarding?: boolean }> = ({
  children,
  requireOnboarding = true,
}) => {
  const { isAuthenticated, user } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/" replace />;
  if (requireOnboarding && user && !user.onboardingComplete) {
    return <Navigate to="/onboarding" replace />;
  }
  return <>{children}</>;
};

const UnknownRoute: React.FC = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  return <Navigate to={isAuthenticated ? '/dashboard' : '/'} replace />;
};

function App() {
  return (
    <ErrorBoundary>
      <Suspense fallback={<LoadingSpinner fullScreen label="Loading Cortex Mail…" />}>
        <Routes>
        {/* Public Routes */}
        <Route path="/"                element={<LandingPage />} />
        <Route path="/auth/callback"   element={<AuthCallbackPage />} />
        <Route path="/privacy"         element={<PrivacyPolicyPage />} />

        {/* Protected Routes */}
        <Route path="/onboarding"      element={<ProtectedRoute requireOnboarding={false}><OnboardingPage /></ProtectedRoute>} />

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

        {/* Unknown paths: signed-in users stay in the app; everyone else sees the landing. */}
        <Route path="*"                element={<UnknownRoute />} />
        </Routes>
      </Suspense>
    </ErrorBoundary>
  );
}

export default App;
