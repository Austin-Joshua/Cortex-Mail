# Nexora Production Deployment Guide

This guide covers deploying Nexora to production on Vercel (frontend) and Render (backend).

---

## Pre-Deployment Checklist

### Code Quality
- [ ] All TypeScript compiles without errors: `npm run build`
- [ ] No unused imports or variables
- [ ] All console.log statements removed
- [ ] Error boundaries implemented
- [ ] Loading states on all async operations

### Security
- [ ] No secrets in code (use `.env` only)
- [ ] Google OAuth app published (not in test mode)
- [ ] JWT_SECRET is 32+ characters
- [ ] ENCRYPTION_KEY is exactly 16 characters
- [ ] CORS_ALLOWED_ORIGINS set to production URLs only
- [ ] H2_CONSOLE_ENABLED=false in production
- [ ] Email notifications contain no sensitive data

### Testing
- [ ] Gmail sync works end-to-end
- [ ] Email classification returns correct categories
- [ ] Brain Q&A responds with referenced emails
- [ ] Notifications trigger and display correctly
- [ ] Calendar events created for deadlines
- [ ] Search filters emails correctly
- [ ] Dark mode toggle persists
- [ ] PWA installs correctly
- [ ] All keyboard shortcuts work
- [ ] Mobile UI is responsive

### Database
- [ ] MySQL 8.0+ is available
- [ ] Database created and accessible
- [ ] Connection string tested
- [ ] Backup plan documented

### Environment Variables
- [ ] All `.env.example` variables configured
- [ ] GOOGLE_CLIENT_ID & SECRET from published app
- [ ] JWT_SECRET and ENCRYPTION_KEY set securely
- [ ] GEMINI_API_KEY configured
- [ ] API URLs have no trailing slashes
- [ ] CORS origins include production domains

---

## Step 1: Frontend Deployment (Vercel)

### 1.1 Connect Repository

1. Go to [Vercel.com](https://vercel.com)
2. Click **Add New** → **Project**
3. Select GitHub repository: `Austin-Joshua/nexora`
4. Click **Import**

### 1.2 Configure Build Settings

```
Root Directory:        nexora/frontend
Framework:             Vite
Build Command:         npm run build
Output Directory:      dist
```

Vercel auto-detects these, but verify in project settings.

### 1.3 Set Environment Variables

Go to **Settings** → **Environment Variables** and add:

```
VITE_API_BASE_URL = https://your-render-backend-url.onrender.com
VITE_GOOGLE_CLIENT_ID = your_google_client_id
```

**Important:** `VITE_API_BASE_URL` must NOT have `/api` suffix and must NOT have trailing slash.

### 1.4 Deploy

1. Click **Deploy**
2. Wait for build to complete (usually 2-3 minutes)
3. Test the URL in browser
4. Verify features work:
   - [ ] Login button works
   - [ ] Search bar focuses with `/`
   - [ ] Dark mode toggle works
   - [ ] PWA install prompt appears

---

## Step 2: Backend Deployment (Render)

### 2.1 Create Web Service

1. Go to [Render.com](https://render.com)
2. Click **New +** → **Web Service**
3. Connect GitHub repository: `Austin-Joshua/nexora`

### 2.2 Configure Service

```
Name:                  nexora-backend
Root Directory:        nexora/backend
Environment:           Docker (or Java if available)
Build Command:         ./mvnw clean package -DskipTests
Start Command:         java -jar target/nexora-backend-0.0.1-SNAPSHOT.jar
```

### 2.3 Set Environment Variables

Click **Environment** and add all from backend `.env.example`:

```
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URI=https://your-render-backend.onrender.com/api/auth/google/callback
JWT_SECRET=your-32-char-secret-key
ENCRYPTION_KEY=your16charkey
CORS_ALLOWED_ORIGINS=https://your-vercel-frontend.vercel.app
GEMINI_API_KEY=your-gemini-key
GEMINI_API_KEY=your-gemini-key (optional)
DB_URL=mysql://user:pass@host:3306/nexora_db
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_DIALECT=org.hibernate.dialect.MySQLDialect
PORT=8080
H2_CONSOLE_ENABLED=false
```

### 2.4 Deploy

1. Click **Create Web Service**
2. Wait for build (first deploy takes 5-10 minutes)
3. Verify in Render dashboard: Service should show "Live"
4. Copy the service URL (e.g., `https://nexora-backend-xxxxx.onrender.com`)

### 2.5 Update Frontend CORS

Go back to Vercel frontend project:
1. Settings → Environment Variables
2. Update `VITE_API_BASE_URL` to the Render URL
3. Redeploy frontend

---

## Step 3: Database Setup (MySQL)

### Option A: Railway (Recommended)

1. Go to [Railway.app](https://railway.app)
2. Create new project
3. Add MySQL service
4. Copy connection string
5. Use as `DB_URL` in backend `.env`

### Option B: PlanetScale (MySQL compatible)

1. Create account at [PlanetScale.com](https://planetscale.com)
2. Create new database
3. Get connection string (MySQL format)
4. Use as `DB_URL` in backend `.env`

### Option C: Self-hosted MySQL

```bash
# On your server
mysql -u root -p -e "CREATE DATABASE nexora_db;"
mysql -u root -p -e "CREATE USER 'nexora'@'%' IDENTIFIED BY 'strong_password';"
mysql -u root -p -e "GRANT ALL PRIVILEGES ON nexora_db.* TO 'nexora'@'%';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

Connection string:
```
jdbc:mysql://your-host:3306/nexora_db?useSSL=true&requireSSL=true
```

### Verify Connection

Backend should auto-create tables via Hibernate `ddl-auto: update`. Check logs:

```bash
# In Render dashboard → Logs
# Look for: "HHH000412: Hibernate is in auto update mode"
# And: "Created sequence [hibernate_sequence]"
```

---

## Step 4: Google Cloud Configuration

### 4.1 Update OAuth Redirect URI

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your Nexora project
3. Go to **APIs & Services** → **Credentials**
4. Click your OAuth 2.0 Web Application
5. Add authorized redirect URI:
   ```
   https://your-render-backend.onrender.com/api/auth/google/callback
   ```
6. Save

### 4.2 Publish OAuth Consent Screen

1. Go to **OAuth consent screen**
2. Click **Publish App**
3. This allows all Google users to login (not just test users)
4. If you need Advanced Protection support, submit for verification (takes 3-7 days)

---

## Step 5: Post-Deployment Testing

### 5.1 Frontend Tests

```bash
# Check that frontend loads
curl -s https://your-vercel-frontend.vercel.app | head -20

# Check PWA manifest
curl https://your-vercel-frontend.vercel.app/manifest.json
```

### 5.2 Backend Tests

```bash
# Health check
curl https://your-render-backend.onrender.com/actuator/health

# OAuth callback test (opens browser)
open "https://your-render-backend.onrender.com/api/auth/google/callback?code=test&state=test"

# Test email endpoint (requires token)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://your-render-backend.onrender.com/api/emails
```

### 5.3 Full Flow Test

1. Open frontend URL in browser
2. Click "Connect Gmail Account"
3. Authorize Google OAuth
4. Select role
5. Watch dashboard load
6. Click dashboard → emails should appear
7. Open Brain page → try a query
8. Check notifications page
9. Try search, dark mode, keyboard shortcuts

---

## Monitoring & Maintenance

### Logging

**Vercel Frontend:**
- Go to project → **Logs**
- Filter by error/warn

**Render Backend:**
- Go to service → **Logs**
- Real-time streaming of Spring Boot logs

### Metrics

**Render:**
- CPU and Memory usage
- Build time
- Uptime percentage

**Vercel:**
- Edge function execution time
- Bandwidth usage

### Alerts

**Render:**
- Service down → Email notification
- Set up uptime monitoring

**Vercel:**
- Build failures → Email notification
- Performance degradation alerts

---

## Scaling Considerations

### Database Optimization

For 10,000+ users:

```sql
-- Add these indexes
CREATE INDEX idx_email_user_received ON emails(user_id, received_at DESC);
CREATE INDEX idx_email_category ON emails(user_id, category);
CREATE INDEX idx_notification_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_action_user_deadline ON email_actions(user_id, deadline);
```

### Backend Scaling

- Increase Render plan size (2GB → 4GB RAM)
- Enable horizontal scaling (multiple instances)
- Use connection pooling (HikariCP configured to 10)
- Enable Redis for session caching (optional)

### Frontend Optimization

- Code splitting via React Router (already implemented)
- Image optimization (no heavy images currently)
- Service worker caching (PWA enabled)

---

## Troubleshooting

### "Cannot find module" on Vercel

```bash
# Check npm cache
npm cache clean --force
npm install

# Redeploy
vercel redeploy
```

### Backend won't start

Check logs on Render:
```
ERROR: Connection refused at localhost:3306
→ Verify DB_URL and credentials

ERROR: GOOGLE_CLIENT_ID not found
→ Check environment variables are set

ERROR: Unable to load encryption key
→ Verify ENCRYPTION_KEY is exactly 16 chars
```

### Gmail sync not working

1. Check GOOGLE_CLIENT_SECRET is correct
2. Verify OAuth app is published
3. Check CORS_ALLOWED_ORIGINS includes frontend domain
4. Check backend logs for API errors

### Notifications not arriving

1. Check WebSocket connection: Browser DevTools → Network → WS
2. Verify CORS allows frontend domain
3. Check notification service is running:
   ```bash
   # In Render logs, search for "Notification"
   ```
4. Manually trigger sync: Dashboard → refresh → sync button

### Calendar events not created

1. Check calendar sync is enabled in Settings
2. Verify Google Calendar API is enabled in Cloud Console
3. Check backend logs for CalendarService errors
4. Try enabling in Settings → AI & Sync → toggle Calendar Sync

---

## Rollback Plan

If production breaks:

1. **Frontend:** Vercel has automatic rollback; click previous deployment
2. **Backend:** Render has deployment history; click redeploy previous version
3. **Database:** Ensure backups are taken daily (Railway/PlanetScale auto-backup)

To rollback Git:
```bash
git revert <commit-hash>
git push origin master
# Platforms auto-redeploy on push
```

---

## Security Hardening

### Before Going Live

- [ ] Change default database passwords
- [ ] Rotate JWT_SECRET monthly
- [ ] Enable 2FA on Google Cloud Console
- [ ] Set up DDoS protection (Cloudflare recommended)
- [ ] Monitor for suspicious login patterns
- [ ] Enable HTTPS only (Vercel/Render handle this)
- [ ] Set security headers in response

### Ongoing

- [ ] Review logs weekly for errors
- [ ] Update dependencies monthly
- [ ] Monitor API rate limits
- [ ] Audit user data access
- [ ] Test backup restoration quarterly
- [ ] Encrypt database backups

---

## Cost Optimization

| Service | Plan | Cost |
|---------|------|------|
| Vercel | Pro | $20/month |
| Render | Standard | $10/month |
| Railway MySQL | Starter | $7/month |
| Google Cloud | Pay-as-you-go | ~$5-20/month |
| Google Gemini | Pay-per-use | ~$0-5/month |
| **Total** | | **~$50-70/month** |

To reduce costs:
- Use H2 database instead of MySQL (free but limited)
- Use Gemini only (no API key cost)
- Use Render free tier (limitations apply)

---

## Success Checklist

- [ ] Frontend deployed and responsive
- [ ] Backend running with no errors
- [ ] Database connected and populated
- [ ] OAuth flow complete end-to-end
- [ ] Gmail sync fetches emails
- [ ] AI classification tags emails
- [ ] Brain Q&A returns answers
- [ ] Notifications push in real-time
- [ ] Calendar events create correctly
- [ ] Keyboard shortcuts functional
- [ ] Dark mode persists
- [ ] PWA installable
- [ ] Search filters work
- [ ] Thread view groups emails
- [ ] Mobile layout responsive

**Congratulations! Nexora is live! 🎉**

---

## Support

For deployment issues:
1. Check Render/Vercel logs
2. Review this guide
3. Search GitHub Issues
4. Create issue with logs and description

