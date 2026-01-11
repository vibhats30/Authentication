# Reusable Authentication Module

[![Version](https://img.shields.io/badge/version-1.2.0-blue.svg)](https://github.com/vibhats30/Authentication)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://reactjs.org/)

A complete, production-ready authentication module with email/password registration, email verification, and OAuth2 social login support (Google, Facebook, GitHub, Twitter). Built with Spring Boot and React.

> **Version 1.2.0** - Email Verification System | [Changelog](CHANGELOG.md) | [Deployment Guide](DEPLOYMENT.md) | [Architecture](ARCHITECTURE-FLOW.md)

## Features

### Authentication Methods
- **Email/Password Registration** with industry-standard password requirements
- **Email Verification** - Users must verify their email address after signup
- **Automated Account Cleanup** - Unverified accounts deleted after 24 hours
- **OAuth2 Social Login** - Google, Facebook, GitHub, Twitter
- **Multi-Device Session Management** with JWT access and refresh tokens
- **Automatic Token Refresh** on expiration

### Security Features
- **Industry-Standard Password Requirements**:
  - Minimum 8 characters
  - At least one uppercase letter
  - At least one lowercase letter
  - At least one digit
  - At least one special character
  - No whitespace allowed

- **Session Management**:
  - JWT access tokens (15 minutes expiration)
  - Refresh tokens (7 days expiration)
  - Multi-device support with device tracking
  - Automatic token refresh mechanism

- **Security Protections**:
  - Rate limiting (20 requests/minute per IP)
  - Account lockout after 5 failed login attempts (24-hour lock)
  - CORS protection
  - Secure password hashing (BCrypt)
  - XSS and injection prevention

### Tech Stack

**Backend:**
- **Java 21 LTS** (Required - Java 25 is not yet fully supported by Lombok)
- Spring Boot 3.2.1
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT (JSON Web Tokens)
- OAuth2 Client
- Lombok 1.18.34

**Frontend:**
- React 18
- TypeScript
- Vite
- Zustand (state management)
- Axios
- React Router

## Project Structure

```
Authentication/
├── src/main/java/com/auth/module/          # Backend (Spring Boot)
│   ├── config/                             # Security, CORS, Rate limiting
│   ├── controller/                         # REST API endpoints
│   ├── model/                              # User, RefreshToken entities
│   ├── repository/                         # JPA repositories
│   ├── security/                           # JWT, OAuth2, User details
│   ├── service/                            # Business logic
│   ├── filter/                             # Rate limiting filter
│   └── exception/                          # Custom exceptions
├── src/main/resources/
│   └── application.yml                     # Application configuration
├── frontend/                               # Frontend (React)
│   ├── src/
│   │   ├── components/                     # Reusable UI components
│   │   ├── pages/                          # Page components
│   │   ├── services/                       # API services
│   │   └── store/                          # State management
│   └── package.json
├── pom.xml                                 # Maven dependencies
└── README.md
```

## Quick Start

### Option 1: Using Startup Scripts (Recommended)

```bash
# 1. Start backend (automatically loads .env variables)
./start-backend.sh

# 2. In a new terminal, start frontend
./start-frontend.sh

# 3. Open http://localhost:3000
```

### Option 2: Manual Start

See detailed setup instructions below.

## Prerequisites

- **Java 21 LTS** (Required - see note below about Java 25)
- Maven 3.6+
- PostgreSQL 12+
- Node.js 18+
- npm or yarn

### Important: Java Version Compatibility

This project requires **Java 21 LTS**. If you have Java 25 installed, Lombok will not work correctly due to compatibility issues.

**If you're on macOS with Java 25 installed:**

Java 21 has been installed at `/opt/homebrew/opt/openjdk@21/`. To use it for this project:

```bash
# Use Java 21 for the current terminal session
source ./use-java-21.sh

# Or manually set JAVA_HOME
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

**To make Java 21 your system default** (requires sudo):
```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

Then verify:
```bash
java -version  # Should show "openjdk version "21.0.9""
```

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE authdb;
```

### 2. Backend Configuration

1. Copy the environment example file:
```bash
cp .env.example .env
```

2. **Generate a secure JWT secret** (256-bit minimum):

   **Option 1 - Using OpenSSL (Recommended):**
   ```bash
   openssl rand -base64 64
   ```

   **Option 2 - Using Node.js:**
   ```bash
   node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
   ```

   **Option 3 - Using Python:**
   ```bash
   python3 -c "import secrets; print(secrets.token_urlsafe(64))"
   ```

   **Option 4 - Online Generator:**
   - Visit: https://generate-secret.vercel.app/64
   - Or use any reputable random key generator

   Copy the generated key for the next step.

3. Configure your `.env` file:
```env
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT Secret (use the generated key from step 2)
JWT_SECRET=<paste-your-generated-secret-here>

# OAuth2 Credentials (get these from OAuth provider consoles)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

FACEBOOK_CLIENT_ID=your-facebook-app-id
FACEBOOK_CLIENT_SECRET=your-facebook-app-secret

GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

TWITTER_CLIENT_ID=your-twitter-client-id
TWITTER_CLIENT_SECRET=your-twitter-client-secret

# Email Configuration (SMTP) - Required for email verification
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
MAIL_FROM_EMAIL=noreply@yourapp.com
MAIL_FROM_NAME=Authentication Module

# Frontend URL for email verification links
FRONTEND_URL=http://localhost:3000

# Email verification token expiration (in hours)
VERIFICATION_TOKEN_EXPIRATION_HOURS=24
```

**Note about Gmail App Passwords:**
If using Gmail, you need to enable 2-step verification and generate an app-specific password:
1. Go to your Google Account settings
2. Navigate to Security > 2-Step Verification
3. At the bottom, select "App passwords"
4. Generate a new app password for "Mail"
5. Use this password in the `MAIL_PASSWORD` field

4. Update `src/main/resources/application.yml` if needed

### 3. OAuth2 Provider Setup

#### Google OAuth2

**Step-by-step guide to create Google OAuth2 credentials:**

1. **Access Google Cloud Console**
   - Navigate to [Google Cloud Console](https://console.cloud.google.com/)
   - Sign in with your Google account

2. **Create a New Project** (or select an existing one)
   - Click the project dropdown at the top of the page
   - Click "New Project"
   - Enter a project name (e.g., "Authentication Module")
   - Click "Create"
   - Wait for the project to be created and select it

3. **Enable Required APIs**
   - In the left sidebar, go to "APIs & Services" > "Library"
   - Search for "Google+ API" or "Google People API"
   - Click on it and press "Enable"
   - Alternatively, search for "Google Identity Services" and enable it

4. **Configure OAuth Consent Screen** (Required before creating credentials)
   - Go to "APIs & Services" > "OAuth consent screen"
   - Select "External" user type (unless you have a Google Workspace)
   - Click "Create"
   - Fill in the required information:
     - **App name**: Your application name (e.g., "Authentication Module")
     - **User support email**: Your email address
     - **Developer contact information**: Your email address
   - Click "Save and Continue"
   - On the "Scopes" page, click "Add or Remove Scopes"
     - Add these scopes:
       - `userinfo.email`
       - `userinfo.profile`
       - `openid`
     - Click "Update" then "Save and Continue"
   - On "Test users" page, click "Save and Continue"
   - Review the summary and click "Back to Dashboard"

5. **Create OAuth 2.0 Credentials**
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "OAuth client ID"
   - Select "Web application" as the application type
   - Configure the OAuth client:
     - **Name**: Give it a descriptive name (e.g., "Auth Module Web Client")
     - **Authorized JavaScript origins**: Add `http://localhost:8080`
     - **Authorized redirect URIs**: Add the following URIs:
       - `http://localhost:8080/oauth2/callback/google`
       - `http://localhost:8080/login/oauth2/code/google` (Spring Security default)
   - Click "Create"

6. **Save Your Credentials**
   - A dialog will appear showing your **Client ID** and **Client Secret**
   - **IMPORTANT**: Copy both values immediately
   - Add them to your `.env` file:
     ```env
     GOOGLE_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
     GOOGLE_CLIENT_SECRET=your-client-secret-here
     ```

7. **For Production Deployment**
   - Go back to "OAuth consent screen"
   - Click "Publish App" to make it available to all users
   - Add your production redirect URIs in the OAuth client settings:
     - `https://yourdomain.com/oauth2/callback/google`
     - `https://yourdomain.com/login/oauth2/code/google`

**Common Issues:**
- If you get "redirect_uri_mismatch" error, ensure the redirect URI in your app exactly matches what's registered in Google Cloud Console
- The consent screen must be configured before creating OAuth credentials
- For testing, you can keep the app in "Testing" mode and add test users manually

#### Facebook OAuth2
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create a new app
3. Add Facebook Login product
4. Add redirect URI: `http://localhost:8080/oauth2/callback/facebook`

#### GitHub OAuth2
1. Go to GitHub Settings > Developer settings > OAuth Apps
2. Create new OAuth App
3. Add callback URL: `http://localhost:8080/oauth2/callback/github`

#### Twitter OAuth2
1. Go to [Twitter Developer Portal](https://developer.twitter.com/)
2. Create a new app
3. Enable OAuth 2.0
4. Add callback URI: `http://localhost:8080/oauth2/callback/twitter`

### 4. Running the Application

#### Start Backend:
```bash
# From project root
mvn clean install
mvn spring-boot:run
```

Backend will run on `http://localhost:8080`

#### Start Frontend:
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend will run on `http://localhost:3000`

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | Register with email/password |
| POST | `/api/auth/login` | Login with email/password |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Logout and revoke refresh token |

### User

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user/me` | Get current user profile |

### OAuth2

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/oauth2/authorize/{provider}` | Initiate OAuth2 login |
| GET | `/oauth2/callback/{provider}` | OAuth2 callback |

## Using as a Reusable Module

### In Your Spring Boot Application

1. Copy the `com.auth.module` package to your project
2. Add dependencies from `pom.xml`
3. Configure `application.yml` with your database and OAuth2 settings
4. Extend security configuration as needed

### In Your React Application

1. Copy the `frontend/src/components` folder (Login, Signup, OAuth2RedirectHandler)
2. Copy `frontend/src/services/authService.ts`
3. Copy `frontend/src/store/authStore.ts`
4. Install dependencies: `react-router-dom`, `axios`, `zustand`
5. Import and use components:

```tsx
import Login from './components/Login';
import Signup from './components/Signup';

function App() {
  return (
    <Login onSuccess={() => console.log('Logged in!')} />
  );
}
```

## Password Requirements

The module enforces NIST-compliant password standards:

- ✅ Minimum 8 characters
- ✅ At least 1 uppercase letter (A-Z)
- ✅ At least 1 lowercase letter (a-z)
- ✅ At least 1 digit (0-9)
- ✅ At least 1 special character (!@#$%^&*...)
- ✅ No whitespace
- ✅ Protection against common passwords
- ✅ Protection against sequential patterns

## Session Management

### Token Lifecycle

1. **Access Token**: 15 minutes (industry standard for high-security apps)
2. **Refresh Token**: 7 days (stored securely in database)
3. Automatic token refresh before expiration
4. Multi-device support - each device gets its own refresh token

### Multi-Device Support

- Each login creates a unique refresh token
- Users can be logged in on multiple devices simultaneously
- Track device info and IP address for each session
- Revoke tokens individually or all at once

## Security Best Practices

1. **Never commit `.env` file** - it contains secrets
2. **Generate strong JWT secret** - use at least 256 bits (64 characters in base64)
   ```bash
   # Quick command to generate a secure secret:
   openssl rand -base64 64
   ```
3. **Use HTTPS in production** - update CORS and OAuth redirect URIs
4. **Rotate JWT secrets periodically** (every 90 days recommended)
5. **Monitor failed login attempts** - investigate patterns
6. **Review active sessions regularly** - check for suspicious devices
7. **Keep dependencies updated** - run `mvn versions:display-dependency-updates`
8. **Use different secrets for different environments** (dev, staging, production)

## Production Deployment

### Backend

1. Update `application.yml` for production:
   - Change database URL
   - Use environment variables for secrets
   - Enable HTTPS
   - Update CORS allowed origins

2. Build production JAR:
```bash
mvn clean package -DskipTests
java -jar target/authentication-module-1.0.0.jar
```

### Frontend

1. Update API URLs in `vite.config.ts`
2. Build production bundle:
```bash
npm run build
```
3. Deploy `dist/` folder to your hosting service

## Customization

### Change Token Expiration

Edit `application.yml`:
```yaml
app:
  auth:
    jwt-expiration-ms: 900000        # 15 minutes
    jwt-refresh-expiration-ms: 604800000  # 7 days
```

### Modify Rate Limits

Edit `RateLimitConfig.java`:
```java
Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
```

### Add Custom User Fields

1. Update `User` entity in `model/User.java`
2. Update database migrations
3. Update DTOs and responses

## Testing

The application includes comprehensive test coverage with unit tests and integration tests.

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=PasswordValidationServiceTest

# Run tests in a specific package
mvn test -Dtest=com.auth.module.service.*
```

### Test Structure

```
src/test/java/com/auth/module/
├── controller/
│   ├── AuthControllerIntegrationTest.java    # API endpoint tests
│   └── UserControllerIntegrationTest.java    # User profile tests
├── service/
│   ├── PasswordValidationServiceTest.java    # Password validation tests
│   └── RefreshTokenServiceTest.java          # Token management tests
└── security/
    └── JwtTokenProviderTest.java             # JWT token tests
```

### Test Coverage

**Unit Tests:**
- ✅ Password validation (17 test cases)
  - Valid/invalid password formats
  - Length requirements
  - Character requirements (uppercase, lowercase, digits, special chars)
  - Whitespace handling

- ✅ JWT token operations (14 test cases)
  - Token generation and validation
  - Token expiration
  - User ID extraction
  - Invalid/malformed token handling

- ✅ Refresh token management (12 test cases)
  - Token creation and verification
  - Token revocation
  - Multi-device support
  - Active token filtering

**Integration Tests:**
- ✅ Authentication endpoints (11 test cases)
  - User registration
  - Login/logout flows
  - Token refresh
  - Input validation

- ✅ User profile endpoints (6 test cases)
  - Authenticated access
  - Token-based security
  - Response validation

### Test Configuration

Tests use H2 in-memory database for isolation:

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
```

### Test Dependencies

- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **Spring Boot Test**: Integration testing
- **H2 Database**: In-memory database for tests
- **MockMvc**: API endpoint testing
- **AssertJ**: Fluent assertions

### Writing New Tests

**Unit Test Example:**
```java
@Test
@DisplayName("Should validate correct password successfully")
void shouldValidateCorrectPassword() {
    // Given
    String validPassword = "SecurePass123!";

    // When
    PasswordValidationResult result =
        passwordValidationService.validatePassword(validPassword);

    // Then
    assertTrue(result.isValid());
}
```

**Integration Test Example:**
```java
@Test
@DisplayName("Should register new user successfully")
void shouldRegisterNewUserSuccessfully() throws Exception {
    // Given
    SignUpRequest request = new SignUpRequest();
    request.setEmail("test@example.com");
    request.setPassword("SecurePass123!");

    // When & Then
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
}
```

### Continuous Integration

Tests are designed to run in CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run tests
  run: mvn clean test

- name: Generate coverage report
  run: mvn jacoco:report
```

## Logging

The application includes comprehensive logging throughout all components with configurable log levels.

### Log Levels

Configure logging in `.env` file or directly in `application.yml`:

```bash
# Set to DEBUG for development, INFO for production
LOG_LEVEL=INFO
LOG_LEVEL_CONTROLLER=INFO
LOG_LEVEL_SERVICE=INFO
LOG_LEVEL_SECURITY=INFO
LOG_LEVEL_FILTER=INFO
```

### Available Log Levels

- **DEBUG**: Detailed information including request parameters, token processing, validation steps
- **INFO**: General operational messages (user actions, successful operations)
- **WARN**: Warning messages (invalid tokens, rate limit violations, validation failures)
- **ERROR**: Error messages with stack traces

### What Gets Logged

**Controllers** (AuthController, UserController):
- Request received (INFO)
- Request parameters and IP addresses (DEBUG)
- Success/failure outcomes (INFO/ERROR)

**Services** (AuthService, RefreshTokenService):
- Operation start/completion (INFO)
- Processing steps (DEBUG)
- Validation failures (WARN)
- Errors with details (ERROR)

**Security** (JwtTokenProvider, JwtAuthenticationFilter):
- Token generation/validation (DEBUG)
- Authentication success/failure (INFO/WARN)
- JWT signature/expiration errors (ERROR)

**Filters** (RateLimitFilter):
- Rate limit checks (DEBUG)
- Rate limit exceeded (WARN)

### Log File Location

Logs are written to:
- **Console**: All environments
- **File**: `logs/authentication-module.log`

Log files automatically rotate:
- Maximum file size: 10MB
- History kept: 30 days

### Sensitive Data Protection

The logging implementation ensures:
- ✅ Passwords are **NEVER** logged
- ✅ Full tokens are **NEVER** logged (only first 10 characters)
- ✅ Sensitive user data is excluded from logs
- ✅ Stack traces only logged at ERROR level

### Example Log Output

```
2026-01-09 10:30:45 - c.a.m.controller.AuthController - Received login request for email: user@example.com
2026-01-09 10:30:45 - c.a.m.service.AuthService - Starting authentication process for email: user@example.com
2026-01-09 10:30:45 - c.a.m.service.RefreshTokenService - Creating refresh token for userId: 123
2026-01-09 10:30:45 - c.a.m.controller.AuthController - User authentication successful for email: user@example.com
```

## Troubleshooting

### Database Connection Failed
- Ensure PostgreSQL is running
- Check database credentials in `.env`
- Verify database exists

### OAuth2 Redirect Not Working
- Check OAuth2 client IDs and secrets
- Verify redirect URIs match in provider console
- Ensure frontend and backend URLs are correct

### Token Refresh Failing
- Check if refresh token is expired or revoked
- Verify JWT secret is consistent
- Check axios interceptor configuration

### Debugging with Logs

For troubleshooting issues:

1. **Enable DEBUG logging**:
   ```bash
   LOG_LEVEL=DEBUG
   LOG_LEVEL_CONTROLLER=DEBUG
   LOG_LEVEL_SERVICE=DEBUG
   ```

2. **Check log file**: `logs/authentication-module.log`

3. **Common patterns**:
   - Authentication failures: Search for "authentication failed"
   - Token issues: Search for "JWT" or "token"
   - Rate limiting: Search for "rate limit exceeded"
   - OAuth2 issues: Search for "OAuth2"

## License

MIT License - feel free to use this in your projects!

## Support

For issues and questions, please check the code comments and configuration files. All major components are well-documented.
