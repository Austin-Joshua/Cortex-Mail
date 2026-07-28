# Nexora Application Test Checklist

## Frontend Build & Deployment Status
- [x] TypeScript compilation without errors
- [x] Vite build successful (no warnings related to application code)
- [x] CSS design system integrated with modern gradients and colors
- [x] All components updated to use CSS variables
- [x] Dark mode support verified in CSS

## 1. Application Loading & Navigation

### 1.1 Frontend Rendering
- [ ] Frontend loads without errors
- [ ] Logo and branding visible
- [ ] All navigation elements present
- [ ] Layout responsive on desktop

### 1.2 Page Navigation (Sidebar)
- [ ] Dashboard link loads dashboard page
- [ ] Inbox link navigates to inbox
- [ ] Priority link shows Priority Inbox
- [ ] Scheduled link navigates to Scheduled page
- [ ] Brain link navigates to AI Brain
- [ ] Drafts link shows Draft management
- [ ] Archive link shows archived emails
- [ ] Shared link shows collaboration interface
- [ ] Analytics link loads analytics page
- [ ] Notifications link shows notifications
- [ ] Settings link shows settings page
- [ ] Help link shows help/FAQ page

## 2. Authentication Flow

### 2.1 Google OAuth
- [ ] "Connect Gmail" button visible on login
- [ ] OAuth flow initiates properly
- [ ] Redirects to Google login page
- [ ] After authorization, returns to dashboard
- [ ] User profile loaded correctly
- [ ] JWT token stored in localStorage
- [ ] Token refresh works on page reload

### 2.2 Logout Functionality
- [ ] Logout button visible in settings
- [ ] Clicking logout clears session
- [ ] Redirects to login page
- [ ] Cannot access protected routes after logout

## 3. Dashboard Page

### 3.1 Stats Display
- [ ] Unread count card displays correct value
- [ ] Deadlines card shows upcoming deadlines count
- [ ] Actions card displays pending actions count
- [ ] "This Week" card shows total email count
- [ ] All stat cards have proper icons and colors
- [ ] Stat cards are clickable (Unread & Deadlines)

### 3.2 Quick Actions
- [ ] "View Priority Inbox" button present and clickable
- [ ] "Ask Nexora Brain" button present and clickable
- [ ] "Compose Email" button present and clickable
- [ ] "View Analytics" button present and clickable
- [ ] Buttons have proper hover effects
- [ ] Button colors match design system

### 3.3 Upcoming Deadlines Widget
- [ ] Displays up to 5 upcoming deadlines
- [ ] Shows deadline title and due date
- [ ] Shows days remaining countdown
- [ ] Proper styling and hover effects
- [ ] Countdown badge visible

### 3.4 AI Insights Widget
- [ ] Shows AI-powered insights message
- [ ] Gradient background applied correctly
- [ ] Icon visible
- [ ] Text displays insights properly

## 4. Email Management

### 4.1 Inbox
- [ ] Lists all emails from Gmail account
- [ ] Shows sender, subject, and snippet
- [ ] Displays read/unread status (bold for unread)
- [ ] Shows email timestamp
- [ ] Emails are clickable
- [ ] Unread count badge shows correct number
- [ ] Sync button in sidebar works
- [ ] Email search functionality works

### 4.2 Email Detail View
- [ ] Full email content displays
- [ ] Sender information visible
- [ ] To, CC, BCC fields shown if applicable
- [ ] Subject line displays
- [ ] Email body renders properly (HTML and plain text)
- [ ] Attachments section visible (if any)
- [ ] Mark as read/unread works
- [ ] Archive functionality works
- [ ] Star/flag functionality works
- [ ] Reply button present

### 4.3 Category Filtering
- [ ] Categories section visible in sidebar when expanded
- [ ] Each category shows count of emails
- [ ] Clicking category filters inbox by that category
- [ ] Category color indicator visible
- [ ] Filter updates email list correctly

## 5. Priority Inbox Feature

### 5.1 Priority Email Detection
- [ ] Priority Inbox page loads
- [ ] Shows AI-prioritized emails
- [ ] Displays high-priority emails first
- [ ] Shows confidence score (if available)
- [ ] Allows marking emails as priority
- [ ] Shows suggestions for prioritization

## 6. Scheduled Emails

### 6.1 Scheduling Interface
- [ ] Scheduled page loads
- [ ] Shows list of scheduled emails
- [ ] Each scheduled email shows send time
- [ ] Allows editing scheduled time
- [ ] Allows canceling scheduled email
- [ ] Shows draft vs scheduled emails

## 7. Drafts Management

### 7.1 Draft Management
- [ ] Drafts page loads
- [ ] Shows list of user drafts
- [ ] Draft auto-save functionality works
- [ ] Can edit existing draft
- [ ] Can delete draft
- [ ] Can schedule draft
- [ ] Shows last modified time

## 8. AI Brain (Question Answering)

### 8.1 Brain Q&A
- [ ] Brain page loads
- [ ] Search/query input visible
- [ ] Can type questions
- [ ] Submit button works
- [ ] Returns relevant results from emails
- [ ] Results are formatted nicely
- [ ] Shows loading state while processing

## 9. Design System & Styling

### 9.1 Color Scheme
- [ ] Primary color (deep indigo #3b4fea) applied correctly
- [ ] Secondary color (teal #00d4aa) visible in UI
- [ ] Accent color (orange #ff6b35) used for CTAs
- [ ] Background colors consistent
- [ ] Text colors readable and accessible
- [ ] Borders use correct color

### 9.2 Dark Mode
- [ ] Dark mode toggle visible (in settings or header)
- [ ] Switching to dark mode works
- [ ] All colors invert properly
- [ ] Text remains readable in dark mode
- [ ] No elements are invisible in dark mode
- [ ] Preference persists on page reload

### 9.3 Animations
- [ ] Page transitions are smooth
- [ ] Hover effects on interactive elements
- [ ] Loading spinners animate
- [ ] Button press animation (scale effect)
- [ ] Fade-in animations on page load
- [ ] Reduced motion preference respected

## 10. Responsive Design

### 10.1 Desktop View (>1024px)
- [ ] Sidebar fully visible
- [ ] Content area has proper spacing
- [ ] Grid layouts display correctly
- [ ] All navigation accessible

### 10.2 Tablet View (768px - 1024px)
- [ ] Sidebar collapses to icon-only
- [ ] Content area expands
- [ ] Touch targets are adequate (44px+)
- [ ] Navigation still accessible

### 10.3 Mobile View (<768px)
- [ ] Bottom navigation visible
- [ ] Content full width
- [ ] Touch-friendly button sizes
- [ ] Modals/drawers work properly
- [ ] Scrolling works smoothly
- [ ] No horizontal scroll

## 11. API Integration

### 11.1 Backend Connectivity
- [ ] Backend service is running
- [ ] API endpoints responding
- [ ] CORS headers configured correctly
- [ ] Token-based auth working
- [ ] Error responses handled gracefully

### 11.2 Gmail API Integration
- [ ] Gmail sync works
- [ ] Emails load into database
- [ ] Categories assigned correctly
- [ ] Timestamps recorded properly
- [ ] Attachments detected

### 11.3 Google Calendar Integration
- [ ] Calendar events load
- [ ] Deadline extraction works
- [ ] Events display in dashboard
- [ ] Calendar API rate limits respected

## 12. Notifications

### 12.1 Email Notifications
- [ ] New email notifications appear
- [ ] Toast messages show for important events
- [ ] Notification center updates
- [ ] Mark notifications as read

### 12.2 WebSocket Real-time
- [ ] WebSocket connection established
- [ ] Real-time sync working
- [ ] Live updates reflected in UI
- [ ] Connection recovers on disconnect

## 13. Settings & Preferences

### 13.1 Settings Page
- [ ] Settings page loads
- [ ] Profile information editable
- [ ] Password change functionality (if applicable)
- [ ] Theme preference saved
- [ ] Notification preferences available
- [ ] Privacy settings visible
- [ ] Logout button functional

## 14. Performance

### 14.1 Load Times
- [ ] Dashboard loads in <3 seconds
- [ ] Email list loads in <2 seconds
- [ ] Search results return in <1 second
- [ ] Brain query processes in reasonable time

### 14.2 Optimization
- [ ] No console errors on page load
- [ ] No network request failures
- [ ] Images optimized
- [ ] Bundle size reasonable

## 15. Accessibility

### 15.1 Keyboard Navigation
- [ ] Tab navigation works
- [ ] Enter key submits forms
- [ ] Esc closes modals
- [ ] Arrow keys work in lists

### 15.2 Screen Reader Compatibility
- [ ] ARIA labels on buttons
- [ ] Form labels associated with inputs
- [ ] Image alt text present
- [ ] Navigation semantic

### 15.3 Color Contrast
- [ ] Text on background meets WCAG AA standards
- [ ] Links distinguishable from text
- [ ] Focus indicators visible

## 16. Error Handling

### 16.1 User Errors
- [ ] Invalid input shows error message
- [ ] Required fields validated
- [ ] Duplicate prevention working
- [ ] File size limits enforced

### 16.2 Network Errors
- [ ] Handles network timeouts gracefully
- [ ] Retry logic works
- [ ] Offline detection implemented
- [ ] Error messages helpful

### 16.3 Authentication Errors
- [ ] Expired token handled
- [ ] Invalid credentials show error
- [ ] Unauthorized access redirects
- [ ] Session restored correctly

## 17. Data Security

### 17.1 Encryption
- [ ] Tokens stored securely
- [ ] HTTPS enforced
- [ ] Sensitive data encrypted
- [ ] No credentials in localStorage

### 17.2 Authorization
- [ ] Users can only see their own emails
- [ ] API validates authorization
- [ ] No data leakage between users
- [ ] Admin features protected

## Test Results Summary
- Total Test Cases: ___
- Passed: ___
- Failed: ___
- Skipped: ___
- Issues Found: ___

## Notes
(Add any observations, bugs found, or recommendations here)

---
**Tester Name:** ___
**Date:** ___
**Environment:** ___
**Browser:** ___
**OS:** ___
