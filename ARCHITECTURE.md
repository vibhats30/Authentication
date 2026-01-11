# Authentication Module - Architecture Documentation

## Overview

This document provides comprehensive documentation of the authentication module's architecture, components, and implementation details. All source files now include inline JavaDoc and comments explaining their purpose and functionality.

## Documentation Status

### ✅ Fully Documented Components

#### Controllers
- **AuthController** - REST API endpoints for authentication operations
  - `POST /api/auth/signup` - User registration with password validation
  - `POST /api/auth/login` - Email/password authentication
  - `POST /api/auth/refresh` - Token refresh mechanism
  - `POST /api/auth/logout` - Device-specific logout

- **UserController** - User profile endpoints
  - `GET /api/user/me` - Get authenticated user profile

#### Services
- **AuthService** - Core authentication business logic
  - User registration with NIST-compliant password validation
  - Login with account lockout (5 attempts, 24-hour lock)
  - Token refresh and logout operations
  - Failed login attempt tracking

- **PasswordValidationService** - NIST-compliant password validation
  - Minimum 8 characters, maximum 128
  - Uppercase, lowercase, digit, special character requirements
  - No whitespace, protection against sequential patterns
  - Detailed error messaging

- **RefreshTokenService** - Multi-device session management
  - UUID-based token generation
  - Device tracking (User-Agent, IP address)
  - Token revocation for logout
  - Support for multiple concurrent sessions

- **CustomUserDetailsService** - Spring Security user loading
  - Loads users by email or ID
  - Integrates with JWT authentication

#### Security Components
- **JwtTokenProvider** - JWT token generation and validation
  - Access token: 15 minutes expiration
  - Refresh token: 7 days expiration
  - HMAC-SHA signature verification
  - Token parsing and validation

- **JwtAuthenticationFilter** - Request authentication filter
  - Extracts JWT from Authorization header
  - Validates token signature and expiration
  - Sets Spring Security authentication context

- **SecurityConfig** - Spring Security configuration
  - Stateless JWT authentication
  - OAuth2 social login configuration
  - BCrypt password encoding
  - CORS and authorization rules

#### Models
- **User** - User entity with security features
  - Email/password and OAuth2 support
  - Account locking and failed attempt tracking
  - Role-based access control
  - Timestamps and audit fields

- **RefreshToken** - Refresh token entity
  - Device-specific token management
  - Expiration and revocation tracking
  - IP address and device info for auditing

- **AuthProvider** - Authentication provider enum
  - LOCAL, GOOGLE, FACEBOOK, GITHUB, TWITTER
  - Prevents authentication method conflicts

#### Configuration
- **SecurityConfig** - Complete security setup
  - JWT filter chain configuration
  - OAuth2 client settings
  - Password encoder (BCrypt)
  - CORS configuration

## Architecture Patterns

### Layered Architecture
```
┌─────────────────────────────────────┐
│         Controllers Layer           │  REST API endpoints
│  (AuthController, UserController)   │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│          Services Layer              │  Business logic
│  (AuthService, RefreshTokenService) │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│        Repository Layer              │  Data access
│   (UserRepository, TokenRepository)  │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│         Database Layer               │  PostgreSQL
│          (Users, Tokens)             │
└──────────────────────────────────────┘
```

### Security Flow

#### Email/Password Authentication
```
1. User submits credentials → AuthController.login()
2. AuthService validates credentials
3. Check account lock status
4. Authenticate with Spring Security
5. Generate JWT access token (15 min)
6. Create refresh token (7 days) with device info
7. Return tokens to user
```

#### OAuth2 Authentication
```
1. User clicks social login → /oauth2/authorize/{provider}
2. Redirect to OAuth2 provider (Google, Facebook, etc.)
3. Provider authenticates user
4. Callback to /oauth2/callback/{provider}
5. CustomOAuth2UserService processes user info
6. Create or update user account
7. OAuth2AuthenticationSuccessHandler generates tokens
8. Redirect to frontend with tokens
```

#### Token Refresh
```
1. Access token expires (after 15 min)
2. Client sends refresh token → AuthController.refresh()
3. RefreshTokenService validates token
4. Check expiration and revocation status
5. Generate new access token
6. Return new access token (refresh token unchanged)
```

## Security Features

### Password Security (NIST Compliant)
- **Minimum Requirements**: 8 characters
- **Complexity**: Uppercase, lowercase, digit, special character
- **Protection**: Against sequential patterns and common passwords
- **Storage**: BCrypt hashing with automatic salt
- **Validation**: Real-time feedback in UI

### Account Security
- **Lockout Mechanism**: 5 failed attempts → 24-hour lock
- **Automatic Unlock**: After lock duration expires
- **Attempt Tracking**: Per-user failed login counter
- **Reset**: Counter reset on successful login

### Session Security
- **Access Tokens**: Short-lived (15 minutes) for security
- **Refresh Tokens**: Long-lived (7 days) stored in database
- **Multi-Device**: Each device gets unique refresh token
- **Revocation**: Device-specific or all-devices logout
- **Tracking**: IP address and device info recorded

### Rate Limiting
- **Implementation**: Bucket4j token bucket algorithm
- **Limit**: 20 requests per minute per IP address
- **Scope**: Authentication endpoints only
- **Response**: HTTP 429 with retry-after header

### CORS Protection
- **Configuration**: Configurable allowed origins
- **Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Credentials**: Supported for cookie-based auth
- **Headers**: All allowed for flexibility

## Multi-Device Session Management

### How It Works
1. **Device Identification**: User-Agent + IP address
2. **Token Isolation**: Each login creates unique refresh token
3. **Independent Sessions**: Tokens don't interfere with each other
4. **Device Logout**: Revoke single token, others remain active
5. **All-Device Logout**: Revoke all tokens for user

### Use Cases
- User logged in on phone, tablet, and laptop simultaneously
- Lost device → revoke token for that device only
- Security concern → revoke all devices, force re-login
- Session management UI → display active devices

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),  -- NULL for OAuth2 users
    name VARCHAR(255) NOT NULL,
    image_url VARCHAR(500),
    email_verified BOOLEAN DEFAULT FALSE,
    provider VARCHAR(20) NOT NULL,  -- LOCAL, GOOGLE, etc.
    provider_id VARCHAR(255),
    account_locked BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    lock_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    last_login TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    role VARCHAR(50) NOT NULL
);
```

### Refresh Tokens Table
```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(255) UNIQUE NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    device_info TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);
```

## Configuration

### Environment Variables
```properties
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT Security
JWT_SECRET=your-256-bit-secret-key
app.auth.jwt-expiration-ms=900000          # 15 minutes
app.auth.jwt-refresh-expiration-ms=604800000  # 7 days

# OAuth2 Providers
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
FACEBOOK_CLIENT_ID=your-facebook-client-id
FACEBOOK_CLIENT_SECRET=your-facebook-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
TWITTER_CLIENT_ID=your-twitter-client-id
TWITTER_CLIENT_SECRET=your-twitter-client-secret

# CORS
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/auth/signup
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response 200:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response 200:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer"
}
```

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}

Response 200:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer"
}
```

#### Logout
```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}

Response 200: "Logged out successfully"
```

### User Endpoints

#### Get Current User
```http
GET /api/user/me
Authorization: Bearer eyJhbGc...

Response 200:
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "imageUrl": "https://...",
  "provider": "LOCAL",
  "roles": ["ROLE_USER"]
}
```

## Frontend Integration

### React Components
All components are fully documented with JSDoc comments:
- **Login** - Email/password and social login
- **Signup** - Registration with real-time password validation
- **OAuth2RedirectHandler** - Handles OAuth2 callbacks
- **Dashboard** - Protected page example

### State Management (Zustand)
- **authStore** - Authentication state with persistence
- Automatic token refresh via Axios interceptors
- Local storage for token persistence
- Token expiration handling

## Testing

### Unit Tests
Document test cases for:
- Password validation rules
- JWT token generation and validation
- Account lockout mechanism
- Token refresh logic

### Integration Tests
Document test scenarios for:
- Complete authentication flow
- OAuth2 integration
- Multi-device sessions
- Rate limiting

## Deployment Considerations

### Production Checklist
- [ ] Change JWT secret to strong random key (256-bit)
- [ ] Enable HTTPS for all communications
- [ ] Update CORS allowed origins for production domains
- [ ] Configure OAuth2 redirect URIs for production
- [ ] Set up database connection pooling
- [ ] Enable database SSL connections
- [ ] Configure proper logging levels
- [ ] Set up monitoring and alerting
- [ ] Implement database backups
- [ ] Review and adjust token expiration times

### Security Best Practices
1. **Never commit secrets** to version control
2. **Rotate JWT secrets** periodically
3. **Monitor failed login attempts** for suspicious activity
4. **Review active sessions** regularly
5. **Keep dependencies updated** for security patches
6. **Use environment variables** for all configuration
7. **Enable database encryption** at rest
8. **Implement API request logging** for auditing

## Maintenance

### Regular Tasks
- Review and clean up expired tokens
- Monitor database growth
- Analyze failed login patterns
- Update OAuth2 provider configurations
- Review and update password policies
- Check for security updates

### Troubleshooting
See README.md for common issues and solutions.

## Contributing

When adding new features:
1. Add comprehensive JavaDoc comments
2. Update this ARCHITECTURE.md file
3. Include unit and integration tests
4. Update README.md if user-facing changes
5. Follow existing code patterns and naming conventions

## License

MIT License - See LICENSE file for details
