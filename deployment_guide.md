# Nexora Deployment & Google OAuth Production Guide

This guide details how to deploy the Nexora application to production on **Render** (backend) and **Vercel** (frontend), and how to verify/publish your Google Cloud project so OAuth works perfectly for everyone (including users with Advanced Protection).

---

## Part 1: Backend Deployment on Render

Render is ideal for hosting the Spring Boot backend. 

### Step-by-Step Setup:
1. Sign in to [Render](https://render.com/).
2. Click **New +** and select **Web Service**.
3. Connect your GitHub repository (`Austin-Joshua/nexora`).
4. Set the following settings:
   - **Name**: `nexora-backend`
   - **Root Directory**: `nexora/backend`
   - **Language**: `Java` or `Docker` (Select **Java** / Native build if supported, or use the Maven Command below).
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -jar target/nexora-backend-0.0.1-SNAPSHOT.jar`
5. Under **Environment Variables**, add the following keys matching your backend `.env.example`:
   - `PORT`: `8080` (Render will override this dynamically)
   - `GOOGLE_CLIENT_ID`: *Your Google OAuth Client ID*
   - `GOOGLE_CLIENT_SECRET`: *Your Google OAuth Client Secret*
   - `GOOGLE_REDIRECT_URI`: `https://YOUR-RENDER-BACKEND-URL.onrender.com/api/auth/google/callback`
   - `JWT_SECRET`: *A secure random string (minimum 32 characters)*
   - `ENCRYPTION_KEY`: *A secure 16-character string*
   - `CORS_ALLOWED_ORIGINS`: `https://YOUR-VERCEL-FRONTEND-URL.vercel.app`
   - `GEMINI_API_KEY`: *Your Google Gemini API key (https://aistudio.google.com)*

---

## Part 2: Frontend Deployment on Vercel

Vercel hosts the React frontend. We have already added a `vercel.json` file to make sure React routing works seamlessly on refresh.

### Step-by-Step Setup:
1. Sign in to [Vercel](https://vercel.com/).
2. Click **Add New** -> **Project**.
3. Import your GitHub repository (`Austin-Joshua/nexora`).
4. In the configuration settings:
   - **Root Directory**: Edit and select `nexora/frontend`.
   - **Build and Output Settings**: Keep default (Vite/React will auto-detect).
5. Under **Environment Variables**, add:
   - `VITE_API_BASE_URL`: `https://YOUR-RENDER-BACKEND-URL.onrender.com`
   - `VITE_GOOGLE_CLIENT_ID`: *Your Google OAuth Client ID*
6. Click **Deploy**.

---

## Part 3: Fixing Google Cloud API Verification & Advanced Protection

Because Nexora requests access to sensitive scopes (`gmail.readonly`, `calendar.events`), Google restricts logins in two ways:
1. Unverified App warning ("This app isn't verified").
2. High-security accounts under **Advanced Protection** will be blocked outright with: `"Access blocked: Nexora is not approved by Advanced Protection"`.

To rectify both issues and make Google OAuth work seamlessly for all users:

### Step 1: Transition App from "Testing" to "In Production"
1. Open the [Google Cloud Console OAuth consent screen](https://console.cloud.google.com/apis/credentials/consent?project=nexora-platform-500906).
2. Under **Publishing status**, click **Publish App**.
3. This lifts the "Test Users" limit, meaning *any* user can now attempt to log in (instead of only those manually added to the test list).

### Step 2: Request Verification from Google (Required for Public Access & Advanced Protection)
Under Google's security model, **users enrolled in Advanced Protection cannot connect their Gmail account to unverified applications**. Only Google-verified applications are trusted by default.
1. On the OAuth Consent Screen page, click **Submit for Verification**.
2. You will need to provide:
   - A **Privacy Policy URL** (you can create a simple static privacy policy page on your website or host a markdown file).
   - An explanation of why you need Gmail read-only access (e.g., "To help students analyze, group, and retrieve deadlines from their university email").
   - A short YouTube video showing how the application uses the OAuth login flow and displays the email data.
3. Once Google approves the verification request (usually takes 3–7 business days), the unverified warning will disappear, and users on Advanced Protection will be able to log in without issues.

### Temporary Workaround for testing right now:
If you need to test with an account enrolled in Advanced Protection immediately without waiting for verification:
*   You must temporarily **Unenroll** that specific account from Advanced Protection via [Google Account Security Settings](https://myaccount.google.com/security) (scroll to "Advanced Protection Program" -> click "Unenroll"). It might take up to 24 hours to sync.
*   Alternatively, test the login using a standard Google account that is not enrolled in Advanced Protection.
