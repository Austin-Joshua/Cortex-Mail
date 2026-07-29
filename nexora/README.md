# 🚀 Nexora — Communication Intelligence Platform

> AI-powered email intelligence for college students. Sits above Gmail, classifies emails, surfaces deadlines, extracts action items, answers natural language questions about your inbox — and now shows **Sender Analytics** with ranked email counts and full content browsing.

![Stack](https://img.shields.io/badge/Backend-Spring_Boot_3-6DB33F?style=flat-square&logo=spring)
![Stack](https://img.shields.io/badge/Frontend-React_18_+_Vite-61DAFB?style=flat-square&logo=react)
![Stack](https://img.shields.io/badge/AI-Gemini_Flash-F15A24?style=flat-square)
![Stack](https://img.shields.io/badge/Auth-Google_OAuth2-4285F4?style=flat-square&logo=google)
![Stack](https://img.shields.io/badge/DB-H2_/_MySQL-003B57?style=flat-square)

---

## Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS |
| State | Zustand + TanStack Query |
| Backend | Spring Boot 3 + Java 17 |
| Database | H2 (dev, zero-config) / MySQL (production) |
| AI | Gemini API (gemini-1.5-flash) |
| Email | Gmail API (read-only) |
| Calendar | Google Calendar API |
| Real-time | WebSocket (STOMP) |

---

## Features

- 📬 **Gmail Sync** — Fetches and stores your inbox with full content
- 🤖 **AI Classification** — Every email auto-tagged: Assignment, Hackathon, Placement, Meeting, etc.
- 🧠 **Nexora Brain** — Ask natural language questions about your inbox
- 🔔 **Smart Alerts** — Role-aware notifications for deadlines and events
- 👤 **Sender Analytics** — See who emails you most, ranked with gold/silver/bronze medals, full drill-down
- 🔒 **Private & Secure** — Read-only Gmail access, AES-256 token encryption

---

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Google Cloud project with Gmail API & Calendar API enabled (OAuth published)
- Google Gemini API key (optional — without it AI falls back to keyword matching)

### 1. Backend
```bash
cd nexora/backend
cp .env.example .env   # fill in your secrets
./mvnw spring-boot:run
# Runs on http://localhost:8080
# Uses H2 in-memory DB by default (no MySQL setup needed for dev)
```

### 2. Frontend
```bash
cd nexora/frontend
cp .env.example .env
npm install
npm run dev
# Runs on http://localhost:5173
```

---

## Google OAuth Setup

> **If you have already published your OAuth app**, all Google accounts can log in — no test user setup needed.

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Enable **Gmail API** and **Google Calendar API**
3. Create **OAuth 2.0 Credentials** → Web Application
4. Add Authorized Redirect URI:
   - Local: `http://localhost:8080/api/auth/google/callback`
   - Production: `https://your-backend-url/api/auth/google/callback`
5. Copy Client ID + Secret into `backend/.env`
6. In **OAuth consent screen** → publish your app (or add test users for testing)

---

## Environment Variables

### `nexora/backend/.env`
```env
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=https://your-backend-url.onrender.com/api/auth/google/callback
JWT_SECRET=your-minimum-32-char-secret-key
ENCRYPTION_KEY=your16charenckey
# Comma-separated: add both Vercel URL and any localhost for local dev
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,http://localhost:5173
# Optional: switch to MySQL (recommended for production data persistence)
# DB_URL=jdbc:mysql://localhost:3306/nexora_db
# DB_USERNAME=root
# DB_PASSWORD=your_password
# DB_DRIVER=com.mysql.cj.jdbc.Driver
# DB_DIALECT=org.hibernate.dialect.MySQLDialect
```

### `nexora/frontend/.env`
```env
# Backend origin — NO trailing slash, NO /api suffix
VITE_API_BASE_URL=https://nexora-75kw.onrender.com
VITE_GOOGLE_CLIENT_ID=your_google_client_id
```

---

## API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/auth/google/callback` | No | OAuth callback → returns JWT |
| GET | `/api/auth/me` | JWT | Current user info |
| PUT | `/api/auth/profile` | JWT | Update role |
| GET | `/api/emails` | JWT | List emails (filterable by category/priority/search) |
| GET | `/api/emails/{id}` | JWT | Full email detail |
| POST | `/api/emails/sync` | JWT | Manual Gmail sync |
| GET | `/api/emails/categories` | JWT | Category counts |
| **GET** | **`/api/emails/by-sender`** | **JWT** | **Ranked sender list with email counts** |
| **GET** | **`/api/emails/sender/{email}`** | **JWT** | **All emails from a specific sender** |
| POST | `/api/brain/query` | JWT | Natural language Q&A |
| GET | `/api/brain/history` | JWT | Past conversations |
| GET | `/api/dashboard/summary` | JWT | Dashboard stats |
| GET | `/api/notifications` | JWT | Notifications list |
| WS | `/ws` | JWT | Real-time push |

---

## Project Structure

```
nexora/
├── README.md
├── frontend/                          # Vite + React + TypeScript + Tailwind
│   ├── index.html
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── package.json
│   └── src/
│       ├── api/
│       │   ├── axiosInstance.ts       # Axios with JWT interceptor
│       │   ├── authApi.ts             # Google OAuth URL builder
│       │   ├── emailApi.ts            # Email CRUD + sender summary
│       │   ├── brainApi.ts            # AI Q&A
│       │   ├── dashboardApi.ts
│       │   └── notificationApi.ts
│       ├── components/
│       │   ├── common/                # LoadingSpinner, ErrorBoundary
│       │   ├── layout/                # AppShell, Sidebar, Navbar
│       │   ├── email/
│       │   │   ├── EmailList.tsx
│       │   │   ├── EmailCard.tsx
│       │   │   ├── EmailDetail.tsx    # Full email content viewer
│       │   │   ├── SenderView.tsx     # 👤 Sender leaderboard + drill-down
│       │   │   ├── CategoryBadge.tsx
│       │   │   └── PriorityBadge.tsx
│       │   ├── brain/
│       │   ├── dashboard/
│       │   └── notifications/
│       ├── hooks/
│       │   ├── useAuth.ts
│       │   ├── useEmails.ts
│       │   ├── useBrain.ts
│       │   └── useWebSocket.ts
│       ├── pages/
│       │   ├── LandingPage.tsx        # + OAuth error banner
│       │   ├── AuthCallbackPage.tsx   # + graceful error handling
│       │   ├── OnboardingPage.tsx
│       │   ├── DashboardPage.tsx
│       │   ├── InboxPage.tsx          # + 👤 Senders tab
│       │   ├── BrainPage.tsx
│       │   ├── NotificationsPage.tsx
│       │   └── SettingsPage.tsx
│       ├── store/                     # Zustand: authStore, emailStore, notificationStore
│       ├── types/                     # Email, User, Notification types
│       └── utils/                     # formatDate, categoryColors, priorityUtils
│
└── backend/                           # Spring Boot 3 + Java 17
    ├── pom.xml
    ├── .env.example
    └── src/main/
        ├── resources/
        │   └── application.yml        # H2 default / MySQL optional
        └── java/com/nexora/
            ├── NexoraApplication.java
            ├── config/
            │   ├── SecurityConfig.java
            │   ├── GmailConfig.java
            │   └── WebSocketConfig.java
            ├── controller/
            │   ├── AuthController.java
            │   ├── EmailController.java   # + by-sender, sender/{email}
            │   ├── BrainController.java
            │   ├── DashboardController.java
            │   └── NotificationController.java
            ├── service/
            │   ├── AuthService.java
            │   ├── EmailService.java      # + getSenderSummary, getEmailsBySender
            │   ├── GmailSyncService.java
            │   ├── EmailClassificationService.java
            │   ├── NexoraBrainService.java
            │   ├── NotificationService.java
            │   └── SummarizationService.java
            ├── model/
            │   ├── User.java
            │   ├── Email.java
            │   ├── EmailAction.java
            │   ├── BrainConversation.java
            │   └── Notification.java
            ├── dto/
            │   ├── request/
            │   │   └── ProfileUpdateRequest.java
            │   └── response/
            │       ├── AuthResponse.java
            │       ├── EmailResponse.java
            │       ├── SenderSummaryResponse.java  # NEW
            │       └── ...
            ├── repository/
            │   ├── EmailRepository.java   # + countBySenderForUser, findByUserIdAndSenderEmail
            │   ├── UserRepository.java
            │   └── ...
            ├── security/
            │   ├── JwtTokenProvider.java
            │   ├── JwtAuthenticationFilter.java
            │   └── TokenEncryptor.java
            └── scheduler/
                └── EmailSyncScheduler.java
```

---

## Security Features

- ✅ AES-256 encryption for Gmail tokens at rest
- ✅ JWT authentication with 24h expiry
- ✅ CORS restricted to configured origins
- ✅ Rate limiting on Brain queries (20/hour via Resilience4j)
- ✅ Gmail read-only scope only
- ✅ All queries scoped to authenticated `userId`
- ✅ H2 console disabled in production

---

## Deployment

| Service | Platform |
|---------|----------|
| Frontend | [Vercel](https://vercel.com) — set `VITE_API_BASE_URL` to your backend URL |
| Backend | [Railway](https://railway.app) or [Render](https://render.com) |
| Database | Railway MySQL / PlanetScale / any managed MySQL |

Set all env vars in your hosting platform matching `.env.example`.  
Add your production URL to **Authorized Redirect URIs** in Google Cloud Console.

---

## License

MIT
