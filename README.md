# 🚀 Velocity — Communication Acceleration Platform

> Accelerate your communication. Reclaim your time. Amplify your impact. Transform how you manage email with AI-powered productivity acceleration.

![Status](https://img.shields.io/badge/status-production--ready-brightgreen)
![Backend](https://img.shields.io/badge/Backend-Spring%20Boot%203-6DB33F?logo=spring)
![Frontend](https://img.shields.io/badge/Frontend-React%2018%2BVite-61DAFB?logo=react)
![Database](https://img.shields.io/badge/Database-MySQL%20%2F%20H2-003B57)
![AI](https://img.shields.io/badge/AI-Claude%2BGemini-F15A24)

---

## Repository layout

```
nexora/
├── frontend/   React + TypeScript web client (Vite)
├── backend/    Spring Boot 3 API (Java 17)
└── mobile/     Flutter native client — see mobile/README.md
```

**Verified state**, so you know what you are picking up:

| | |
|---|---|
| `frontend` — `npm run build` | builds clean |
| `backend` — `mvn compile` | compiles clean (77 classes) |
| `mobile` — `flutter analyze` / `flutter test` | clean / 17 passing |
| `mobile` — `flutter build apk` | **not yet verified on a machine with the Android SDK** |

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Deployment](#deployment)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## ✨ Features

### 📬 Gmail Integration
- **OAuth2 Authentication** — Secure Google login with published OAuth consent
- **Email Sync** — Fetch and store your complete inbox with full content
- **Real-time Sync** — Manual sync button + scheduled background sync (every 5 minutes)
- **Token Management** — Automatic token refresh with 24-hour expiry

### 🤖 AI Intelligence
- **Email Classification** — Auto-tags: Assignment, Hackathon, Placement, Meeting, Announcement, etc.
- **Deadline Detection** — Extracts due dates from email content (ISO 8601 format)
- **Action Items** — Parses actionable tasks with descriptions and deadlines
- **Resilient AI** — Claude → Gemini → Local keyword fallback (works with zero API keys)
- **Thread Summarization** — Claude-powered summaries for email conversations

### 🧠 Velocity Brain
- **Natural Language Q&A** — Ask questions about your inbox, get AI-powered answers
- **Conversation History** — All conversations saved and searchable
- **Referenced Emails** — AI responses link back to relevant emails
- **Acceleration Insights** — Smart suggestions to improve communication velocity

### 📊 Smart Notifications & Analytics
- **Smart Alerts** — Deadlines, action items, important emails
- **Real-time Push** — WebSocket-based instant notifications
- **Analytics Dashboard** — 12-week email volume heatmap, category distribution, sender stats
- **Sender Leaderboard** — See who emails you most with 🥇🥈🥉 rankings

### 🎯 Power User Features
- **Email Search** — Real-time search across senders, subjects, and body
- **Thread View** — Group emails by Gmail thread ID, see entire conversations
- **AI Reply Studio** — Generate professional, formal, friendly, or concise reply drafts
- **Calendar Sync** — Auto-create Google Calendar events for detected deadlines
- **Dark Mode** — System-aware dark/light theme toggle
- **Keyboard Shortcuts** — `/` search, `j/k` navigate, `e` archive, `r` reply, and more
- **PWA Support** — Install as native app on mobile/desktop, offline capability

### 🔒 Security & Privacy
- **Read-only Gmail Access** — Never modifies or sends emails
- **AES-256 Token Encryption** — Tokens encrypted at rest
- **JWT Authentication** — 24-hour expiring sessions
- **CORS Locked** — API restricted to configured origins only
- **Rate Limiting** — Semaphore(10) on AI queries to prevent abuse
- **Data Isolation** — All queries scoped to authenticated user ID

---

## 🏗️ Tech Stack

### Frontend
```
React 18 + TypeScript
├─ Vite (build tool)
├─ Tailwind CSS (styling)
├─ Zustand (state management)
├─ TanStack Query (data fetching)
├─ React Router (navigation)
├─ Recharts (analytics charts)
├─ Lucide Icons (icons)
├─ SockJS + STOMP (WebSocket)
└─ Vite PWA Plugin (offline support)
```

### Backend
```
Spring Boot 3.x + Java 17
├─ Spring Security (OAuth2)
├─ Spring Data JPA (database)
├─ Spring WebSocket (real-time)
├─ Google APIs (Gmail, Calendar)
├─ Anthropic Claude API (AI classification)
├─ Google Gemini API (AI fallback)
├─ Resilience4j (rate limiting)
├─ Lombok (code generation)
└─ Maven (build tool)
```

### Database
```
Development:  H2 (in-memory, zero config)
Production:   MySQL 8.0+
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (JDK)
- **Node.js 18+** (npm)
- **MySQL 8.0+** (optional — defaults to in-memory H2)
- **Google Cloud Project** with Gmail & Calendar APIs enabled
- **Gemini API key** (optional — without it the AI falls back to keyword matching)

### Run the whole stack with one command

```bash
git clone https://github.com/Austin-Joshua/Velocity.git
cd Velocity
./run-dev.sh              # backend :8080 + frontend :5173, Ctrl-C stops both
```

The script checks your toolchain, creates `backend/.env` from the example on
first run, generates throwaway dev secrets if none are set, compiles, waits
for each service to report healthy, and streams logs to `.dev-logs/`.

**To click through the UI before you have Google OAuth credentials:**

```bash
./run-dev.sh --bypass
```

then open <http://localhost:8080/api/auth/bypass> — it signs you in against a
seeded demo account and drops you on the dashboard.

> The demo account's 5 emails carry **pre-written** summaries, not AI output —
> real Gmail sync is deliberately skipped for it. To watch Gemini actually
> classify mail, summarise it and extract action items, sign in with a real
> Google account (see below). `--bypass` is local-only and hands out sessions
> to anyone who can reach the port; never enable it on a deployed instance.

To enable real "Sign in with Google", set `GOOGLE_CLIENT_ID` and
`GOOGLE_CLIENT_SECRET` in `nexora/backend/.env`, and add this exact authorised
redirect URI to your OAuth client in the Google Cloud console:

```
http://localhost:8080/api/auth/google/callback
```

The client id is configured **only** in the backend — the frontend does not
need it, and there is no `VITE_GOOGLE_CLIENT_ID`.

### Manual setup

### 1. Clone & Setup

```bash
git clone https://github.com/Austin-Joshua/nexora.git
cd nexora
```

### 2. Backend Setup

```bash
cd nexora/backend

# Create .env file
cp .env.example .env

# Edit .env with your secrets
nano .env

# Install dependencies & run
./mvnw spring-boot:run
# Backend runs on http://localhost:8080
```

**Required .env variables:**
```env
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/google/callback
JWT_SECRET=your-32-char-minimum-secret-key
ENCRYPTION_KEY=your16charkey

# Optional (for AI classification)
CLAUDE_API_KEY=your_claude_key
GEMINI_API_KEY=your_gemini_key (auto-fallback if Claude not set)

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173

# Production Database (optional)
# DB_URL=jdbc:mysql://localhost:3306/nexora_db
# DB_USERNAME=root
# DB_PASSWORD=your_password
```

### 3. Frontend Setup

```bash
cd nexora/frontend

# Install dependencies
npm install

# Create .env file
cp .env.example .env

# Edit .env
nano .env
```

**Required .env variables:**
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=your_client_id
```

**Start dev server:**
```bash
npm run dev
# Frontend runs on http://localhost:5173
```

### 4. Test the Flow

1. **Open browser** → `http://localhost:5173`
2. **Click "Connect Gmail Account"**
3. **Authorize OAuth consent screen**
4. **Select your role** (Student, Professor, etc.)
5. **Watch Gmail sync** → Emails appear on dashboard
6. **See AI classification** → Categories auto-populated
7. **Try Nexora Brain** → Ask questions about your inbox
8. **Search emails** → Use inbox search bar
9. **View analytics** → Check email volume heatmap

---

## 📁 Project Structure

```
nexora/
├── README.md (this file)
├── nexora/
│   ├── frontend/                              # React + Vite SPA
│   │   ├── src/
│   │   │   ├── api/                           # API clients (emailApi, brainApi, etc.)
│   │   │   ├── components/
│   │   │   │   ├── layout/                    # AppShell, TopBar, Sidebar
│   │   │   │   ├── email/                     # EmailList, EmailDetail, SenderView
│   │   │   │   ├── brain/                     # BrainChat, BrainMessage
│   │   │   │   ├── dashboard/                 # Stats, heatmap, analytics
│   │   │   │   ├── notifications/             # NotificationPanel, items
│   │   │   │   └── common/                    # Reusable UI components
│   │   │   ├── pages/                         # Route pages
│   │   │   ├── hooks/                         # Custom React hooks
│   │   │   ├── store/                         # Zustand state stores
│   │   │   ├── types/                         # TypeScript interfaces
│   │   │   └── utils/                         # Helpers, formatters
│   │   ├── public/
│   │   │   ├── manifest.json                  # PWA manifest
│   │   │   ├── icon-192.png                   # App icons
│   │   │   └── icon-512.png
│   │   ├── vite.config.ts                     # Vite + PWA config
│   │   ├── tailwind.config.js                 # Tailwind setup
│   │   ├── package.json
│   │   └── tsconfig.json
│   │
│   └── backend/                               # Spring Boot API
│       ├── src/main/java/com/nexora/
│       │   ├── NexoraApplication.java         # Main entry point
│       │   ├── config/                        # Security, WebSocket, OAuth
│       │   ├── controller/                    # REST endpoints
│       │   │   ├── AuthController.java        # Login, OAuth callback
│       │   │   ├── EmailController.java       # Email CRUD + search
│       │   │   ├── BrainController.java       # Q&A endpoint
│       │   │   ├── DashboardController.java   # Stats & summary
│       │   │   ├── NotificationController.java # Notifications
│       │   │   ├── AnalyticsController.java   # Analytics data
│       │   │   └── EmailActionController.java # Action item tracking
│       │   ├── service/                       # Business logic
│       │   │   ├── AuthService.java           # Auth logic, JWT
│       │   │   ├── GmailSyncService.java      # Gmail API integration
│       │   │   ├── EmailService.java          # Email CRUD
│       │   │   ├── EmailClassificationService # AI classification + fallback
│       │   │   ├── NexoraBrainService.java    # Q&A logic
│       │   │   ├── CalendarService.java       # Google Calendar sync
│       │   │   ├── NotificationService.java   # Notification logic
│       │   │   ├── SummarizationService.java  # Thread summarization
│       │   │   └── GeminiService.java         # Gemini fallback
│       │   ├── model/                         # JPA entities
│       │   │   ├── User.java
│       │   │   ├── Email.java
│       │   │   ├── EmailAction.java
│       │   │   ├── Notification.java
│       │   │   └── BrainConversation.java
│       │   ├── repository/                    # Spring Data JPA repos
│       │   ├── dto/                           # Request/Response DTOs
│       │   ├── security/                      # JWT, encryption
│       │   │   ├── JwtTokenProvider.java
│       │   │   ├── TokenEncryptor.java        # AES-256
│       │   │   └── JwtAuthenticationFilter.java
│       │   └── scheduler/                     # Background jobs
│       │       └── EmailSyncScheduler.java
│       ├── src/main/resources/
│       │   ├── application.yml                # Spring config
│       │   ├── application-prod.yml           # Production overrides
│       │   └── db/migration/                  # Flyway migrations
│       ├── pom.xml                            # Maven dependencies
│       └── .env.example
│
└── deployment_guide.md                        # Production deployment
```

---

## ⚙️ Configuration

### Gmail OAuth Setup (Required)

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing
3. **Enable APIs:**
   - Gmail API
   - Google Calendar API
4. **Create OAuth Credentials:**
   - Type: Web Application
   - Authorized redirect URIs:
     - Local: `http://localhost:8080/api/auth/google/callback`
     - Production: `https://your-backend-url.onrender.com/api/auth/google/callback`
5. Copy **Client ID** and **Client Secret** to `.env`
6. Publish OAuth consent screen (or add test users for testing)

### AI Keys (Optional)

**Claude (Recommended):**
```bash
CLAUDE_API_KEY=sk-ant-... # From https://console.anthropic.com
```

**Gemini (Fallback):**
```bash
GEMINI_API_KEY=... # From https://aistudio.google.com
```

If neither key is set, Nexora uses local keyword-based classification (still works, just less intelligent).

### Database Configuration

**Development (Default - H2):**
- Zero configuration required
- Data stored in-memory
- Resets on app restart

**Production (MySQL):**
```env
DB_URL=jdbc:mysql://your-host:3306/nexora_db
DB_USERNAME=your_user
DB_PASSWORD=your_password
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_DIALECT=org.hibernate.dialect.MySQLDialect
```

---

## 📡 API Reference

### Authentication
```
POST /api/auth/google/callback
  Query params: code, state
  Returns: JWT token + user info

GET /api/auth/me
  Headers: Authorization: Bearer <token>
  Returns: Current user profile

PUT /api/auth/profile
  Headers: Authorization: Bearer <token>
  Body: { role: "STUDENT", calendarSyncEnabled: true }
  Returns: Updated user profile

POST /api/auth/logout
  Headers: Authorization: Bearer <token>
```

### Emails
```
GET /api/emails
  Query params: category, priority, search, page, size
  Headers: Authorization: Bearer <token>
  Returns: EmailPage with paginated results

GET /api/emails/:id
  Headers: Authorization: Bearer <token>
  Returns: Full email detail with AI metadata

POST /api/emails/sync
  Headers: Authorization: Bearer <token>
  Triggers manual Gmail sync

GET /api/emails/categories
  Headers: Authorization: Bearer <token>
  Returns: Count of emails per category

GET /api/emails/by-sender
  Headers: Authorization: Bearer <token>
  Returns: List of senders with email counts

GET /api/emails/sender/:email
  Headers: Authorization: Bearer <token>
  Returns: All emails from specific sender

GET /api/emails/thread/:threadId
  Headers: Authorization: Bearer <token>
  Returns: All emails in Gmail thread

PATCH /api/emails/:id/read
  Headers: Authorization: Bearer <token>
  Marks email as read

POST /api/emails/:id/draft-reply
  Headers: Authorization: Bearer <token>
  Body: { style: "PROFESSIONAL" | "FORMAL" | "FRIENDLY" | "CONCISE" }
  Returns: Generated reply draft
```

### Nexora Brain
```
POST /api/brain/query
  Headers: Authorization: Bearer <token>
  Body: { query: "What assignments are due?" }
  Returns: { response: "...", referencedEmails: [...] }

GET /api/brain/history
  Headers: Authorization: Bearer <token>
  Returns: List of past conversations
```

### Dashboard
```
GET /api/dashboard/summary
  Headers: Authorization: Bearer <token>
  Returns: {
    unreadCount: 5,
    upcomingDeadlines: [...],
    pendingActions: [...],
    categoryCounts: {...}
  }

GET /api/dashboard/email-volume?days=7
  Headers: Authorization: Bearer <token>
  Returns: Daily email counts for last N days
```

### Notifications
```
GET /api/notifications
  Headers: Authorization: Bearer <token>
  Returns: List of notifications

PATCH /api/notifications/:id/read
  Headers: Authorization: Bearer <token>
  Marks notification as read
```

### WebSocket (Real-time)
```
WS /ws
  Headers: Authorization: Bearer <token>
  Subscribe: /user/queue/notifications
  Receives: Real-time notification events
```

---

## 🗄️ Database Schema

### Users
```sql
users
├── id (PK)
├── email
├── name
├── role (STUDENT, PROFESSOR, etc.)
├── gmail_access_token (AES-256 encrypted)
├── gmail_refresh_token (AES-256 encrypted)
├── token_expiry
├── calendar_sync_enabled (boolean)
├── last_synced_at (timestamp)
└── created_at, updated_at
```

### Emails
```sql
emails
├── id (PK)
├── user_id (FK)
├── gmail_message_id (unique per user)
├── gmail_thread_id
├── subject
├── sender_email
├── sender_name
├── received_at
├── body_snippet
├── body_full
├── category (ASSIGNMENT, HACKATHON, etc.)
├── priority (HIGH, MEDIUM, LOW)
├── ai_summary
├── deadline_detected (datetime, nullable)
├── is_deadline_added_to_calendar
├── is_read
├── ai_action_items (JSON)
└── created_at, updated_at
```

### Email Actions
```sql
email_actions
├── id (PK)
├── email_id (FK)
├── user_id (FK)
├── action_description
├── deadline (datetime, nullable)
├── is_completed
└── created_at, updated_at
```

### Brain Conversations
```sql
brain_conversations
├── id (PK)
├── user_id (FK)
├── user_query
├── ai_response
├── referenced_email_ids (JSON)
└── created_at
```

### Notifications
```sql
notifications
├── id (PK)
├── user_id (FK)
├── title
├── message
├── notification_type (DEADLINE, ACTION_REQUIRED, etc.)
├── related_email_id (FK, nullable)
├── is_read
└── created_at
```

---

## 🚀 Deployment

### Frontend (Vercel)

1. **Connect GitHub** → Import `Austin-Joshua/nexora`
2. **Root Directory:** `nexora/frontend`
3. **Build Command:** `npm run build`
4. **Environment Variables:**
   ```
   VITE_API_BASE_URL=https://your-backend-url.onrender.com
   VITE_GOOGLE_CLIENT_ID=your_client_id
   ```
5. **Deploy**

### Backend (Render or Railway)

**Render:**
1. Create new Web Service
2. Connect GitHub repo
3. **Root Directory:** `nexora/backend`
4. **Build Command:** `./mvnw clean package -DskipTests`
5. **Start Command:** `java -jar target/nexora-backend-0.0.1-SNAPSHOT.jar`
6. **Environment Variables:** (see `.env.example`)
7. **Deploy**

**Railway:**
1. Add MySQL database
2. Create Java service
3. Connect GitHub repo
4. Set environment variables
5. Deploy

### Database (Production)

**Option 1: Railway MySQL** (recommended)
```bash
# Railway provides MySQL_URL automatically
# Spring Boot auto-configures from DATABASE_URL
```

**Option 2: PlanetScale (MySQL compatible)**
```bash
DB_URL=mysql://user:pass@pscale_host/dbname?sslMode=VERIFY_IDENTITY
```

**Option 3: Self-hosted MySQL**
```bash
DB_URL=jdbc:mysql://your-host:3306/nexora_db?useSSL=true
```

---

## ✅ Testing

### Manual Testing Checklist

**Authentication:**
- [ ] Google login works
- [ ] JWT token stored in localStorage
- [ ] Token refreshes before expiry
- [ ] Logout clears token
- [ ] Protected routes redirect to login

**Gmail Sync:**
- [ ] Manual sync button works
- [ ] Emails appear in inbox
- [ ] New emails synced in 5 minutes
- [ ] Email deduplication works (no duplicates)
- [ ] Full email body loads

**AI Classification:**
- [ ] Categories auto-assigned
- [ ] Deadlines detected and formatted
- [ ] Action items extracted
- [ ] Role affects suggestions
- [ ] Works with no API keys (local fallback)

**Search & Filter:**
- [ ] Real-time search works
- [ ] Category tabs filter emails
- [ ] Unread toggle works
- [ ] Sender view shows top contacts

**Brain Q&A:**
- [ ] Brain suggestions appear
- [ ] Query gets AI response
- [ ] Referenced emails shown
- [ ] Conversation history saved
- [ ] History sidebar loads past conversations

**Notifications:**
- [ ] Deadline notifications trigger
- [ ] Notifications clickable → navigate to email
- [ ] Real-time push via WebSocket
- [ ] Mark as read works
- [ ] Unread count updates

**Calendar Integration:**
- [ ] Calendar sync toggle works
- [ ] Event created for deadline
- [ ] Event has correct date/time
- [ ] Event description includes sender

**UI/UX:**
- [ ] Dark mode toggle works
- [ ] Theme persists on reload
- [ ] Search bar focusable with `/`
- [ ] Keyboard shortcuts work (j, k, e, r)
- [ ] Mobile responsive
- [ ] PWA installable

### Automated Testing

```bash
# Frontend
cd nexora/frontend
npm run lint        # TypeScript type check
npm run build       # Production build

# Backend
cd nexora/backend
./mvnw test         # Run unit tests
./mvnw clean package # Full build with tests
```

---

## 🐛 Troubleshooting

### "Cannot find module" errors
```bash
cd nexora/frontend
npm install
npm run build
```

### TypeScript errors
```bash
# Clear cache and rebuild
rm -rf dist/ node_modules/
npm install
npm run build
```

### Gmail sync not working
1. Check `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in `.env`
2. Verify redirect URI matches Google Cloud Console
3. Check that Gmail API is enabled in Google Cloud
4. See backend logs: `./mvnw spring-boot:run`

### AI classification always uses local fallback
1. Set `CLAUDE_API_KEY` or `GEMINI_API_KEY`
2. Verify API key is valid
3. Check rate limits (20 req/hour default)
4. See backend logs for API errors

### Notifications not working
1. Ensure WebSocket connection: Check browser DevTools → Network → WS
2. Verify `CORS_ALLOWED_ORIGINS` includes frontend URL
3. Check backend logs for WebSocket errors

### Database connection errors
```bash
# For MySQL
mysql -u root -p -e "CREATE DATABASE nexora_db;"
# Then set DB_URL in .env
```

### CORS errors
1. Update `CORS_ALLOWED_ORIGINS` to match frontend URL
2. For production: `https://your-vercel-frontend.vercel.app`
3. Restart backend after changing `.env`

---

## 📊 Performance Tips

### Frontend Optimization
- PWA caching reduces load time on repeat visits
- Image lazy loading for email screenshots
- Code splitting via React Router
- Gzip compression on Vercel

### Backend Optimization
- Email sync runs async (doesn't block)
- Database indexes on frequently queried fields
- Rate limiting prevents API abuse
- Thread summarization runs in background

### Database Optimization
```sql
-- Add these indexes for production
CREATE INDEX idx_email_user_received ON emails(user_id, received_at DESC);
CREATE INDEX idx_email_gmail_message ON emails(user_id, gmail_message_id);
CREATE INDEX idx_notification_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_action_user_email ON email_actions(user_id, email_id);
```

---

## 📝 Contributing

To contribute to Nexora:

1. **Fork the repository**
2. **Create a feature branch:** `git checkout -b feature/your-feature`
3. **Make your changes** (follow existing code style)
4. **Test thoroughly** (see Testing section)
5. **Commit with clear message:** `git commit -m "feat: description"`
6. **Push to branch:** `git push origin feature/your-feature`
7. **Create Pull Request** with description of changes

### Code Style
- **Frontend:** TypeScript, no `any` types, use interfaces
- **Backend:** Java 17, follow Spring Boot conventions, use Lombok
- **Commits:** Conventional Commits (`feat:`, `fix:`, `docs:`, etc.)

---

## 📄 License

MIT License - see LICENSE file for details.

---

## 🤝 Support

- **Issues:** GitHub Issues (provide details, logs, steps to reproduce)
- **Discussions:** GitHub Discussions for questions
- **Email:** See SUPPORT.md for contact info

---

## 🙏 Acknowledgments

- **Google APIs** for Gmail & Calendar integration
- **Anthropic Claude** & **Google Gemini** for AI
- **Spring Boot** & **React** communities
- All contributors and users

---

**Happy emailing with Nexora! 🚀**

Built with ❤️ for students and professionals who care about productivity.
