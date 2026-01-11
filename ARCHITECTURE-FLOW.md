# Authentication Module - Architecture Flow

## Table of Contents
1. [System Overview](#system-overview)
2. [Component Architecture](#component-architecture)
3. [Authentication Flows](#authentication-flows)
4. [Request/Response Flow](#requestresponse-flow)
5. [Database Schema](#database-schema)
6. [Container Architecture](#container-architecture)
7. [CI/CD Pipeline](#cicd-pipeline)

---

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Authentication Module v1.0.0                │
│                                                                 │
│  Frontend (React)  ←→  Backend (Spring Boot)  ←→  Database     │
│                                                                 │
│  OAuth2 Providers  ←→  JWT Tokens  ←→  Security Layer          │
└─────────────────────────────────────────────────────────────────┘
```

### Technology Stack

**Frontend:**
- React 18 with TypeScript
- Zustand (State Management)
- Axios (HTTP Client)
- React Router (Navigation)
- Vite (Build Tool)
- Nginx (Production Server)

**Backend:**
- Spring Boot 3.2.1
- Spring Security OAuth2 Client
- JWT (JSON Web Tokens)
- Apache Tomcat (Embedded Server)
- Java 21
- Maven (Build Tool)

**Database:**
- PostgreSQL 16
- JPA/Hibernate (ORM)

**DevOps:**
- Docker & Docker Compose
- GitHub Actions (CI/CD)
- Multi-stage builds

---

## Component Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT BROWSER                             │
│                         (http://localhost:5173)                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ HTTP/HTTPS Requests
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                         NGINX (Production)                          │
│                    or Vite Dev Server (Development)                 │
│                                                                      │
│  • Serves React static files                                        │
│  • Routes /api/* to backend                                         │
│  • Handles SPA routing (fallback to index.html)                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ Proxy: /api/*, /oauth2/*
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                    SPRING BOOT APPLICATION                          │
│                      (Apache Tomcat :8080)                          │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │              SECURITY FILTER CHAIN                         │   │
│  │                                                             │   │
│  │  1. CorsFilter                                             │   │
│  │  2. JwtAuthenticationFilter (Custom)                       │   │
│  │  3. OAuth2AuthorizationRequestRedirectFilter              │   │
│  │  4. OAuth2LoginAuthenticationFilter                        │   │
│  │  5. ExceptionTranslationFilter                             │   │
│  │  6. FilterSecurityInterceptor                              │   │
│  └────────────────────────────────────────────────────────────┘   │
│                               │                                      │
│  ┌────────────────────────────▼────────────────────────────────┐   │
│  │                    CONTROLLERS                              │   │
│  │                                                             │   │
│  │  • AuthController          (/api/auth/*)                   │   │
│  │    - register()            POST /register                  │   │
│  │    - login()               POST /login                     │   │
│  │    - refreshToken()        POST /refresh                   │   │
│  │    - logout()              POST /logout                    │   │
│  │                                                             │   │
│  │  • UserController          (/api/user/*)                   │   │
│  │    - getCurrentUser()      GET /me                         │   │
│  │    - getUserProfile()      GET /{id}                       │   │
│  │    - updateUser()          PUT /{id}                       │   │
│  │    - deleteUser()          DELETE /{id}                    │   │
│  │                                                             │   │
│  │  • OAuth2Controller        (/oauth2/*)                     │   │
│  │    - redirectHandler()     GET /redirect                   │   │
│  └────────────────────────────┬────────────────────────────────┘   │
│                               │                                      │
│  ┌────────────────────────────▼────────────────────────────────┐   │
│  │                      SERVICES                               │   │
│  │                                                             │   │
│  │  • AuthService                                             │   │
│  │    - registerUser()                                        │   │
│  │    - authenticateUser()                                    │   │
│  │    - generateTokens()                                      │   │
│  │    - refreshAccessToken()                                  │   │
│  │    - validatePassword()                                    │   │
│  │    - checkAccountLockout()                                 │   │
│  │                                                             │   │
│  │  • UserService                                             │   │
│  │    - findById()                                            │   │
│  │    - findByEmail()                                         │   │
│  │    - updateUser()                                          │   │
│  │    - deleteUser()                                          │   │
│  │                                                             │   │
│  │  • TokenService                                            │   │
│  │    - generateAccessToken()                                 │   │
│  │    - generateRefreshToken()                                │   │
│  │    - validateToken()                                       │   │
│  │    - extractClaims()                                       │   │
│  │                                                             │   │
│  │  • OAuth2UserService (Custom)                              │   │
│  │    - loadUser()                                            │   │
│  │    - processOAuth2User()                                   │   │
│  │    - registerOrUpdateUser()                                │   │
│  └────────────────────────────┬────────────────────────────────┘   │
│                               │                                      │
│  ┌────────────────────────────▼────────────────────────────────┐   │
│  │                    REPOSITORIES                             │   │
│  │                                                             │   │
│  │  • UserRepository (JPA)                                    │   │
│  │    - findByEmail()                                         │   │
│  │    - findByProvider()                                      │   │
│  │    - existsByEmail()                                       │   │
│  │                                                             │   │
│  │  • RefreshTokenRepository (JPA)                            │   │
│  │    - findByToken()                                         │   │
│  │    - deleteByUserId()                                      │   │
│  │    - deleteExpiredTokens()                                 │   │
│  └────────────────────────────┬────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ JDBC
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                      POSTGRESQL DATABASE                            │
│                                                                      │
│  Tables:                                                            │
│  • users                                                            │
│  • refresh_tokens                                                   │
│                                                                      │
│  Port: 5432                                                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Authentication Flows

### 1. Email/Password Registration Flow

```
┌──────┐                                                    ┌────────┐
│Client│                                                    │Backend │
└───┬──┘                                                    └───┬────┘
    │                                                           │
    │ 1. POST /api/auth/register                               │
    │    { email, password, name }                             │
    ├──────────────────────────────────────────────────────────>│
    │                                                           │
    │                                      2. Validate Password │
    │                                         (min 8 chars,     │
    │                                          uppercase,       │
    │                                          lowercase,       │
    │                                          digit, special)  │
    │                                                           │
    │                                    3. Check Email Exists? │
    │                                        ┌──────────────────┤
    │                                        │ Query Database   │
    │                                        └──────────────────>│
    │                                                           │
    │                                      4. Hash Password     │
    │                                         (BCrypt)          │
    │                                                           │
    │                                      5. Create User       │
    │                                        ┌──────────────────┤
    │                                        │ INSERT INTO      │
    │                                        │ users            │
    │                                        └──────────────────>│
    │                                                           │
    │                                      6. Generate Tokens   │
    │                                         - Access Token    │
    │                                         - Refresh Token   │
    │                                                           │
    │                                      7. Save Refresh Token│
    │                                        ┌──────────────────┤
    │                                        │ INSERT INTO      │
    │                                        │ refresh_tokens   │
    │                                        └──────────────────>│
    │                                                           │
    │ 8. Return { user, accessToken, refreshToken }            │
    │<──────────────────────────────────────────────────────────┤
    │                                                           │
    │ 9. Store tokens in localStorage                          │
    │ 10. Redirect to dashboard                                │
    │                                                           │
```

### 2. Email/Password Login Flow

```
┌──────┐                                                    ┌────────┐
│Client│                                                    │Backend │
└───┬──┘                                                    └───┬────┘
    │                                                           │
    │ 1. POST /api/auth/login                                  │
    │    { email, password }                                   │
    ├──────────────────────────────────────────────────────────>│
    │                                                           │
    │                                      2. Find User by Email│
    │                                        ┌──────────────────┤
    │                                        │ SELECT * FROM    │
    │                                        │ users            │
    │                                        │ WHERE email=?    │
    │                                        └──────────────────>│
    │                                                           │
    │                                      3. Check Lockout     │
    │                                         (5 failed attempts│
    │                                          = 24hr lock)     │
    │                                                           │
    │                                      4. Verify Password   │
    │                                         BCrypt.matches()  │
    │                                                           │
    │                              ┌───────────────────────────┐│
    │                              │ If Invalid:               ││
    │                              │ - Increment failed_count  ││
    │                              │ - Lock if count >= 5      ││
    │                              │ - Return 401 Unauthorized ││
    │                              └───────────────────────────┘│
    │                                                           │
    │                                      5. Reset Failed Count│
    │                                         (on success)      │
    │                                                           │
    │                                      6. Generate Tokens   │
    │                                         - Access Token    │
    │                                         - Refresh Token   │
    │                                                           │
    │                                      7. Save Refresh Token│
    │                                        ┌──────────────────┤
    │                                        │ INSERT INTO      │
    │                                        │ refresh_tokens   │
    │                                        └──────────────────>│
    │                                                           │
    │                                      8. Update last_login │
    │                                        ┌──────────────────┤
    │                                        │ UPDATE users     │
    │                                        │ SET last_login   │
    │                                        └──────────────────>│
    │                                                           │
    │ 9. Return { user, accessToken, refreshToken }            │
    │<──────────────────────────────────────────────────────────┤
    │                                                           │
    │ 10. Store tokens in localStorage                         │
    │ 11. Redirect to dashboard                                │
    │                                                           │
```

### 3. OAuth2 Social Login Flow (Google Example)

```
┌──────┐           ┌────────┐           ┌────────┐           ┌──────┐
│Client│           │Backend │           │ Google │           │  DB  │
└───┬──┘           └───┬────┘           └───┬────┘           └───┬──┘
    │                  │                    │                    │
    │ 1. Click "Login with Google"         │                    │
    ├─────────────────>│                    │                    │
    │                  │                    │                    │
    │ 2. Redirect to Google OAuth2          │                    │
    │    /oauth2/authorize/google           │                    │
    │<─────────────────┤                    │                    │
    │                  │                    │                    │
    │ 3. User redirected to Google login    │                    │
    ├──────────────────────────────────────>│                    │
    │                  │                    │                    │
    │                  │     4. User authenticates with Google   │
    │                  │                    │                    │
    │ 5. Google redirects with authorization code                │
    │    https://backend/oauth2/callback/google?code=AUTH_CODE   │
    │<──────────────────────────────────────┤                    │
    │                  │                    │                    │
    │ 6. Forward callback to backend        │                    │
    ├─────────────────>│                    │                    │
    │                  │                    │                    │
    │                  │ 7. Exchange code for access token       │
    │                  ├───────────────────>│                    │
    │                  │                    │                    │
    │                  │ 8. Return access token                  │
    │                  │<───────────────────┤                    │
    │                  │                    │                    │
    │                  │ 9. Request user info with token         │
    │                  ├───────────────────>│                    │
    │                  │                    │                    │
    │                  │ 10. Return user profile                 │
    │                  │     { email, name, picture }            │
    │                  │<───────────────────┤                    │
    │                  │                    │                    │
    │                  │ 11. Find or create user                 │
    │                  │                    │                    │
    │                  │ 12. Check if user exists by email       │
    │                  ├───────────────────────────────────────>│
    │                  │                    │                    │
    │                  │ 13. If not exists, create new user      │
    │                  │     provider = GOOGLE                   │
    │                  │     email_verified = true               │
    │                  ├───────────────────────────────────────>│
    │                  │                    │                    │
    │                  │ 14. If exists, update user info         │
    │                  │     (name, image_url)                   │
    │                  ├───────────────────────────────────────>│
    │                  │                    │                    │
    │                  │ 15. Generate JWT tokens                 │
    │                  │     - Access Token                      │
    │                  │     - Refresh Token                     │
    │                  │                    │                    │
    │                  │ 16. Save refresh token                  │
    │                  ├───────────────────────────────────────>│
    │                  │                    │                    │
    │                  │ 17. Redirect to frontend with token     │
    │                  │     /oauth2/redirect?token=JWT_TOKEN    │
    │<─────────────────┤                    │                    │
    │                  │                    │                    │
    │ 18. Frontend extracts token from URL  │                    │
    │ 19. Store token in localStorage       │                    │
    │ 20. Fetch user profile                │                    │
    │ 21. Redirect to dashboard             │                    │
    │                  │                    │                    │
```

### 4. Token Refresh Flow

```
┌──────┐                                                    ┌────────┐
│Client│                                                    │Backend │
└───┬──┘                                                    └───┬────┘
    │                                                           │
    │ 1. API call with expired access token                    │
    ├──────────────────────────────────────────────────────────>│
    │                                                           │
    │ 2. Token validation fails (expired)                      │
    │    Return 401 Unauthorized                               │
    │<──────────────────────────────────────────────────────────┤
    │                                                           │
    │ 3. Axios interceptor catches 401                         │
    │ 4. POST /api/auth/refresh                                │
    │    { refreshToken }                                      │
    ├──────────────────────────────────────────────────────────>│
    │                                                           │
    │                                      5. Validate Refresh  │
    │                                         Token             │
    │                                        ┌──────────────────┤
    │                                        │ SELECT * FROM    │
    │                                        │ refresh_tokens   │
    │                                        │ WHERE token=?    │
    │                                        └──────────────────>│
    │                                                           │
    │                                      6. Check Expiration  │
    │                                                           │
    │                              ┌───────────────────────────┐│
    │                              │ If expired or invalid:    ││
    │                              │ - Delete token            ││
    │                              │ - Return 401              ││
    │                              │ - Force re-login          ││
    │                              └───────────────────────────┘│
    │                                                           │
    │                                      7. Generate New      │
    │                                         Access Token      │
    │                                                           │
    │                                      8. Rotate Refresh    │
    │                                         Token (optional)  │
    │                                        ┌──────────────────┤
    │                                        │ UPDATE           │
    │                                        │ refresh_tokens   │
    │                                        └──────────────────>│
    │                                                           │
    │ 9. Return { accessToken, refreshToken }                  │
    │<──────────────────────────────────────────────────────────┤
    │                                                           │
    │ 10. Update tokens in localStorage                        │
    │ 11. Retry original API call with new token               │
    │                                                           │
```

### 5. Logout Flow

```
┌──────┐                                                    ┌────────┐
│Client│                                                    │Backend │
└───┬──┘                                                    └───┬────┘
    │                                                           │
    │ 1. POST /api/auth/logout                                 │
    │    Authorization: Bearer <access_token>                  │
    │    { refreshToken }                                      │
    ├──────────────────────────────────────────────────────────>│
    │                                                           │
    │                                      2. Validate Access   │
    │                                         Token             │
    │                                                           │
    │                                      3. Delete Refresh    │
    │                                         Token             │
    │                                        ┌──────────────────┤
    │                                        │ DELETE FROM      │
    │                                        │ refresh_tokens   │
    │                                        │ WHERE token=?    │
    │                                        └──────────────────>│
    │                                                           │
    │                                      4. Optionally delete │
    │                                         all user tokens   │
    │                                        ┌──────────────────┤
    │                                        │ DELETE FROM      │
    │                                        │ refresh_tokens   │
    │                                        │ WHERE user_id=?  │
    │                                        └──────────────────>│
    │                                                           │
    │ 5. Return success                                        │
    │<──────────────────────────────────────────────────────────┤
    │                                                           │
    │ 6. Clear localStorage                                    │
    │ 7. Clear Zustand state                                   │
    │ 8. Redirect to login page                                │
    │                                                           │
```

---

## Request/Response Flow

### Protected API Request Flow

```
┌──────┐                                                    ┌────────┐
│Client│                                                    │Backend │
└───┬──┘                                                    └───┬────┘
    │                                                           │
    │ 1. GET /api/user/me                                      │
    │    Authorization: Bearer eyJhbGci...                     │
    ├──────────────────────────────────────────────────────────>│
    │                                                           │
    │                           ┌───────────────────────────────┤
    │                           │ 2. CorsFilter                 │
    │                           │    - Check origin             │
    │                           │    - Add CORS headers         │
    │                           └───────────────────────────────┤
    │                                                           │
    │                           ┌───────────────────────────────┤
    │                           │ 3. JwtAuthenticationFilter    │
    │                           │    - Extract token from header│
    │                           │    - Validate token signature │
    │                           │    - Check expiration         │
    │                           │    - Extract user info        │
    │                           │    - Set SecurityContext      │
    │                           └───────────────────────────────┤
    │                                                           │
    │                           ┌───────────────────────────────┤
    │                           │ 4. FilterSecurityInterceptor  │
    │                           │    - Check if user has        │
    │                           │      required authorities     │
    │                           └───────────────────────────────┤
    │                                                           │
    │                           ┌───────────────────────────────┤
    │                           │ 5. UserController.getCurrentUser()│
    │                           │    - Get user from SecurityContext│
    │                           │    - Call UserService         │
    │                           └───────────────────────────────┤
    │                                                           │
    │                           ┌───────────────────────────────┤
    │                           │ 6. UserService.findById()     │
    │                           │    - Call UserRepository      │
    │                           └───────────────────────────────┤
    │                                                           │
    │                           ┌───────────────────────────────┤
    │                           │ 7. UserRepository             │
    │                           │    - Query database           │
    │                           │    SELECT * FROM users        │
    │                           │    WHERE id = ?               │
    │                           └───────────────────────────────┤
    │                                                           │
    │                           ┌───────────────────────────────┤
    │                           │ 8. Map Entity to DTO          │
    │                           │    - Remove sensitive fields  │
    │                           │      (password, etc.)         │
    │                           └───────────────────────────────┤
    │                                                           │
    │ 9. Return { user: { id, email, name, ... } }             │
    │<──────────────────────────────────────────────────────────┤
    │                                                           │
```

### Rate Limiting Flow

```
┌──────┐                                                    ┌────────┐
│Client│                                                    │Backend │
└───┬──┘                                                    └───┬────┘
    │                                                           │
    │ Multiple requests from same IP                           │
    ├──────────────────────────────────────────────────────────>│
    │                           ┌───────────────────────────────┤
    │                           │ RateLimitFilter (Bucket4j)    │
    │                           │                               │
    │                           │ - Track requests per IP       │
    │                           │ - Limit: 20 requests/minute   │
    │                           │ - Use token bucket algorithm  │
    │                           │                               │
    │                           │ If limit exceeded:            │
    │                           │   Return 429 Too Many Requests│
    │                           │   Header: Retry-After: 60     │
    │                           └───────────────────────────────┤
    │                                                           │
    │ If within limit: Process request normally                │
    │<──────────────────────────────────────────────────────────┤
    │                                                           │
```

---

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255),              -- NULL for OAuth users
    name              VARCHAR(100) NOT NULL,
    image_url         TEXT,                      -- Profile picture URL
    email_verified    BOOLEAN DEFAULT FALSE,
    provider          VARCHAR(50) DEFAULT 'LOCAL', -- LOCAL, GOOGLE, FACEBOOK, etc.
    provider_id       VARCHAR(255),              -- OAuth provider user ID
    role              VARCHAR(20) DEFAULT 'USER', -- USER, ADMIN
    enabled           BOOLEAN DEFAULT TRUE,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked_until TIMESTAMP,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login        TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider, provider_id);
```

### Refresh Tokens Table

```sql
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),                 -- Device name/type
    ip_address  VARCHAR(45),                  -- IPv4 or IPv6
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### Entity Relationships

```
┌─────────────────────────────────┐
│           users                 │
├─────────────────────────────────┤
│ id (PK)                         │
│ email (UNIQUE)                  │
│ password                        │
│ name                            │
│ image_url                       │
│ provider                        │
│ provider_id                     │
│ role                            │
│ enabled                         │
│ failed_login_attempts           │
│ account_locked_until            │
│ created_at                      │
│ updated_at                      │
│ last_login                      │
└─────────────────┬───────────────┘
                  │
                  │ 1:N
                  │
┌─────────────────▼───────────────┐
│       refresh_tokens            │
├─────────────────────────────────┤
│ id (PK)                         │
│ user_id (FK) → users.id         │
│ token (UNIQUE)                  │
│ device_info                     │
│ ip_address                      │
│ expires_at                      │
│ created_at                      │
│ last_used                       │
└─────────────────────────────────┘
```

---

## Container Architecture

### Docker Compose Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Docker Network: auth-network               │
│                                                                      │
│  ┌────────────────────┐     ┌────────────────────┐                 │
│  │   Frontend         │     │   Backend          │                 │
│  │   Container        │────>│   Container        │                 │
│  │                    │     │                    │                 │
│  │ - Nginx            │     │ - Spring Boot      │                 │
│  │ - React SPA        │     │ - Tomcat           │                 │
│  │ - Port: 80         │     │ - Port: 8080       │                 │
│  │                    │     │                    │                 │
│  └────────────────────┘     └──────────┬─────────┘                 │
│           │                             │                           │
│           │                             │                           │
│           │                             │                           │
│           │                  ┌──────────▼─────────┐                 │
│           │                  │   PostgreSQL       │                 │
│           │                  │   Container        │                 │
│           │                  │                    │                 │
│           │                  │ - PostgreSQL 16    │                 │
│           │                  │ - Port: 5432       │                 │
│           │                  │ - Volume: pgdata   │                 │
│           │                  │                    │                 │
│           │                  └────────────────────┘                 │
│           │                                                          │
└───────────┼──────────────────────────────────────────────────────────┘
            │
            │ Port Mapping
            │
┌───────────▼──────────────────────────────────────────────────────────┐
│                          Host Machine                                │
│                                                                       │
│  - localhost:5173 → Frontend (dev)                                   │
│  - localhost:80 → Frontend (prod)                                    │
│  - localhost:8080 → Backend                                          │
│  - localhost:5432 → PostgreSQL                                       │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### Multi-Stage Docker Build

#### Backend Build Process

```
┌────────────────────────────────────────────────────────────┐
│                    Stage 1: BUILD                          │
│                                                            │
│  Base Image: maven:3.9-eclipse-temurin-21-alpine          │
│                                                            │
│  1. COPY pom.xml                                          │
│  2. RUN mvn dependency:go-offline                         │
│     (Download all dependencies - cached layer)            │
│  3. COPY src/                                             │
│  4. RUN mvn clean package -DskipTests                     │
│     (Build JAR file)                                      │
│                                                            │
│  Output: target/authentication-module-1.0.0.jar           │
└────────────────────────────┬───────────────────────────────┘
                             │
                             │ Copy artifact
                             │
┌────────────────────────────▼───────────────────────────────┐
│                    Stage 2: RUNTIME                        │
│                                                            │
│  Base Image: eclipse-temurin:21-jre-alpine                │
│  (Much smaller - no build tools)                          │
│                                                            │
│  1. Create non-root user 'spring'                         │
│  2. COPY --from=build target/*.jar app.jar                │
│  3. Switch to 'spring' user                               │
│  4. EXPOSE 8080                                           │
│  5. ENTRYPOINT ["java", "-jar", "app.jar"]                │
│                                                            │
│  Final Image Size: ~250MB (vs ~800MB with build stage)    │
└────────────────────────────────────────────────────────────┘
```

#### Frontend Build Process

```
┌────────────────────────────────────────────────────────────┐
│                    Stage 1: BUILD                          │
│                                                            │
│  Base Image: node:20-alpine                               │
│                                                            │
│  1. COPY package*.json                                    │
│  2. RUN npm ci --only=production                          │
│     (Install dependencies - cached layer)                 │
│  3. COPY src/, public/, vite.config.ts, etc.              │
│  4. RUN npm run build                                     │
│     (Build optimized production bundle)                   │
│                                                            │
│  Output: dist/ (static files)                             │
└────────────────────────────┬───────────────────────────────┘
                             │
                             │ Copy static files
                             │
┌────────────────────────────▼───────────────────────────────┐
│                    Stage 2: RUNTIME                        │
│                                                            │
│  Base Image: nginx:1.25-alpine                            │
│  (Tiny web server - ~40MB)                                │
│                                                            │
│  1. COPY --from=build dist/ /usr/share/nginx/html/        │
│  2. COPY nginx.conf /etc/nginx/conf.d/default.conf        │
│  3. EXPOSE 80                                             │
│  4. CMD ["nginx", "-g", "daemon off;"]                    │
│                                                            │
│  Final Image Size: ~45MB (vs ~500MB with build stage)     │
└────────────────────────────────────────────────────────────┘
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TRIGGER EVENT                               │
│                                                                      │
│  - Push to 'main' branch                                            │
│  - Push to 'develop' branch                                         │
│  - Pull request to 'main'                                           │
│  - Manual workflow dispatch                                         │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                         STAGE 1: TEST                               │
│                                                                      │
│  ┌──────────────────────┐       ┌──────────────────────┐           │
│  │  Backend Tests       │       │  Frontend Tests      │           │
│  │                      │       │                      │           │
│  │ - Setup Java 21      │       │ - Setup Node 20      │           │
│  │ - Setup PostgreSQL   │       │ - npm install        │           │
│  │ - mvn clean test     │       │ - npm run test       │           │
│  │ - Generate coverage  │       │ - Generate coverage  │           │
│  │                      │       │                      │           │
│  │ If fails: STOP       │       │ If fails: STOP       │           │
│  └──────────────────────┘       └──────────────────────┘           │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ Both pass
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                         STAGE 2: BUILD                              │
│                                                                      │
│  ┌──────────────────────┐       ┌──────────────────────┐           │
│  │  Build Backend Image │       │  Build Frontend Image│           │
│  │                      │       │                      │           │
│  │ - docker build       │       │ - docker build       │           │
│  │ - Tag: latest        │       │ - Tag: latest        │           │
│  │ - Tag: {sha}         │       │ - Tag: {sha}         │           │
│  │ - Tag: v1.0.0        │       │ - Tag: v1.0.0        │           │
│  │                      │       │                      │           │
│  └──────────┬───────────┘       └───────────┬──────────┘           │
│             │                               │                       │
│             │    ┌──────────────────────┐   │                       │
│             └───>│ Push to Docker Hub   │<──┘                       │
│                  │                      │                           │
│                  │ - Login to registry  │                           │
│                  │ - Push all tags      │                           │
│                  └──────────────────────┘                           │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ Build successful
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                     STAGE 3: DEPLOY                                 │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  Determine Environment:                                    │    │
│  │                                                            │    │
│  │  - If branch = 'develop' → Deploy to DEVELOPMENT          │    │
│  │  - If branch = 'main' → Deploy to TEST → PRODUCTION       │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌──────────────────────┐                                          │
│  │   Development        │                                          │
│  │   (Auto Deploy)      │                                          │
│  │                      │                                          │
│  │ - No approval needed │                                          │
│  │ - Pull images        │                                          │
│  │ - docker-compose up  │                                          │
│  │ - Run smoke tests    │                                          │
│  └──────────────────────┘                                          │
│                                                                      │
│  ┌──────────────────────┐                                          │
│  │   Test               │                                          │
│  │   (Optional Approval)│                                          │
│  │                      │                                          │
│  │ - Pull images        │                                          │
│  │ - Deploy to test env │                                          │
│  │ - Run integration    │                                          │
│  │   tests              │                                          │
│  └──────────┬───────────┘                                          │
│             │                                                       │
│             │ Tests pass                                            │
│             │                                                       │
│  ┌──────────▼───────────┐                                          │
│  │   Production         │                                          │
│  │   (REQUIRES APPROVAL)│                                          │
│  │                      │                                          │
│  │ 1. Notify team       │                                          │
│  │ 2. Wait for approval │                                          │
│  │ 3. Create backup     │                                          │
│  │ 4. Pull images       │                                          │
│  │ 5. Deploy with       │                                          │
│  │    zero-downtime     │                                          │
│  │ 6. Health checks     │                                          │
│  │ 7. Notify success    │                                          │
│  └──────────────────────┘                                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Environment-Specific Configuration

```
Development Environment
├── Branch: develop
├── Database: authdb_dev
├── URL: https://dev.example.com
├── Auto-deploy: Yes
├── Approval: No
├── Debug logging: Enabled
└── OAuth2: Test credentials

Test Environment
├── Branch: main (after merge)
├── Database: authdb_test
├── URL: https://test.example.com
├── Auto-deploy: Yes
├── Approval: Optional
├── Debug logging: Enabled
└── OAuth2: Test credentials

Production Environment
├── Branch: main (manual trigger or tag)
├── Database: authdb_prod
├── URL: https://example.com
├── Auto-deploy: No
├── Approval: Required
├── Debug logging: Disabled
├── Error details: Hidden
└── OAuth2: Production credentials
```

---

## Security Layers

### Multi-Layer Security Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Layer 1: Network                           │
│                                                                      │
│  - CORS Protection                                                  │
│  - Rate Limiting (20 req/min per IP)                                │
│  - HTTPS/SSL (production)                                           │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                          Layer 2: Authentication                    │
│                                                                      │
│  - JWT Token Validation                                             │
│  - OAuth2 Authorization                                             │
│  - Password Hashing (BCrypt)                                        │
│  - Account Lockout (5 attempts → 24hr lock)                         │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                          Layer 3: Authorization                     │
│                                                                      │
│  - Role-Based Access Control (USER, ADMIN)                          │
│  - Resource-level permissions                                       │
│  - Method security (@PreAuthorize)                                  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                          Layer 4: Data Protection                   │
│                                                                      │
│  - Password validation rules                                        │
│  - Sensitive data filtering                                         │
│  - SQL injection prevention (JPA)                                   │
│  - XSS protection (Content-Type headers)                            │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                          Layer 5: Monitoring                        │
│                                                                      │
│  - Failed login tracking                                            │
│  - Session management                                               │
│  - Audit logging                                                    │
│  - Health checks                                                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Component Interaction Summary

### Key Interactions

1. **Frontend → Backend**
   - REST API calls
   - JWT token in Authorization header
   - Automatic token refresh

2. **Backend → Database**
   - JPA/Hibernate ORM
   - Connection pooling
   - Transaction management

3. **Backend → OAuth2 Providers**
   - Authorization code flow
   - Token exchange
   - User info retrieval

4. **Backend ↔ JWT**
   - Token generation (sign)
   - Token validation (verify)
   - Claims extraction

5. **Docker Containers**
   - Frontend container → Backend container (HTTP)
   - Backend container → PostgreSQL container (JDBC)
   - All on same Docker network

6. **CI/CD Pipeline**
   - GitHub → GitHub Actions
   - GitHub Actions → Docker Hub
   - Docker Hub → Deployment servers

---

## Technology Decisions & Rationale

### Why These Technologies?

**Spring Boot 3.2.1**
- Industry standard for Java backend
- Comprehensive security framework
- Built-in OAuth2 support
- Excellent documentation

**React 18 + TypeScript**
- Modern, component-based UI
- Type safety with TypeScript
- Large ecosystem
- Easy state management

**PostgreSQL**
- ACID compliance
- Reliable and mature
- Good performance
- Open source

**JWT (JSON Web Tokens)**
- Stateless authentication
- Scalable (no server-side sessions)
- Industry standard
- Works well with microservices

**Docker**
- Consistent environments
- Easy deployment
- Isolation
- Portability

**GitHub Actions**
- Native to GitHub
- Free for public repos
- Easy to configure
- Good ecosystem

---

## Performance Optimizations

### Backend

1. **Connection Pooling** - HikariCP for efficient database connections
2. **JPA Lazy Loading** - Fetch only needed data
3. **Caching** - In-memory token cache
4. **HTTP/2** - Enabled in production
5. **Compression** - Gzip enabled for responses

### Frontend

1. **Code Splitting** - Lazy load routes
2. **Minification** - Vite production build
3. **Tree Shaking** - Remove unused code
4. **Asset Optimization** - Compressed images and fonts
5. **CDN Ready** - Static assets can be served from CDN

### Database

1. **Indexes** - On frequently queried columns
2. **Connection Pooling** - Reuse connections
3. **Query Optimization** - JPA query hints
4. **Schema Design** - Normalized structure

---

## Monitoring & Observability

### Health Checks

```
Backend Health Endpoint: /actuator/health
├── Database: Connected
├── Disk Space: Available
└── Memory: Within limits

Frontend Health: HTTP 200 on /
└── Nginx serving static files

Database Health: pg_isready
└── PostgreSQL accepting connections
```

### Logging Levels

**Development:**
- Root: INFO
- Application: DEBUG
- Spring Security: DEBUG

**Production:**
- Root: WARN
- Application: INFO
- Spring Security: WARN

---

## Deployment Topology

### Local Development

```
Developer Machine
├── Frontend: Vite dev server (localhost:5173)
├── Backend: Spring Boot (localhost:8080)
└── Database: PostgreSQL (localhost:5432)
```

### Docker Compose Deployment

```
Single Host
├── Docker Network: auth-network
│   ├── Frontend Container (:80)
│   ├── Backend Container (:8080)
│   └── PostgreSQL Container (:5432)
└── Docker Volumes
    └── pgdata (persisted database)
```

### Production Deployment (Example)

```
Cloud Infrastructure
├── Load Balancer (HTTPS)
│   └── Routes traffic to frontend and backend
├── Frontend Servers (N instances)
│   └── Nginx + React SPA
├── Backend Servers (N instances)
│   └── Spring Boot + Tomcat
└── Database Cluster
    ├── Primary PostgreSQL
    └── Replica(s) for read scaling
```

---

## Conclusion

This authentication module provides:

✅ **Secure** - Multiple security layers, industry best practices
✅ **Scalable** - Stateless JWT authentication, Docker containers
✅ **Maintainable** - Clean architecture, comprehensive documentation
✅ **Production-Ready** - CI/CD pipeline, multi-environment support
✅ **Modern** - Latest technologies, TypeScript, Java 21
✅ **Extensible** - Easy to add new features (2FA, email verification, etc.)

---

*Generated: 2026-01-11*
*Version: 1.0.0*
*For: Authentication Module*
