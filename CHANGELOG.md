# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-01-11

### Added
- **Email Verification System** - Comprehensive email verification for new user accounts
  - Users must verify their email address after registration
  - Verification emails sent automatically on signup
  - 24-hour token expiration for security
  - Accounts remain disabled until email is verified
  - Resend verification email functionality

- **Email Templates**
  - Professional HTML email templates using Thymeleaf
  - Verification email with clickable link and 24-hour expiration notice
  - Welcome email sent after successful verification
  - Account deletion warning email (12 hours before expiration)

- **Automated Account Cleanup**
  - Scheduled job runs daily at 2 AM
  - Automatically deletes unverified accounts after 24 hours
  - Deletes expired verification tokens
  - Warning emails sent 12 hours before account deletion

- **New API Endpoints**
  - `GET /api/auth/verify-email?token={token}` - Verify email address
  - `POST /api/auth/resend-verification` - Resend verification email

- **Frontend Components**
  - Email verification page with status indicators
  - Verification pending message after signup
  - Resend verification email button
  - Automatic redirect to login after successful verification

### Enhanced
- **User Entity** - Added `enabled` field to track account activation status
- **Security** - Users cannot access protected resources until email is verified
- **Email Service** - Integrated Spring Boot Mail with SMTP support
- **Configuration** - Added email settings to .env and application-dev.yml

### Technical Details
- VerificationToken entity with UUID tokens and expiration tracking
- Spring @Scheduled annotations for automated cleanup tasks
- @EnableScheduling enabled in main application class
- Thymeleaf template engine for HTML email rendering
- SMTP configuration with TLS support
- Transaction management for atomic operations

### Configuration
- Configurable token expiration time (default: 24 hours)
- SMTP server settings (host, port, username, password)
- Email sender information (from address and name)
- Frontend URL for verification links

## [1.1.0] - 2026-01-11

### Added
- **Startup Scripts** for simplified application launch
  - `start-backend.sh` - Automatically loads environment variables from .env and starts backend
  - `start-frontend.sh` - Starts frontend development server
  - Scripts handle Java 21 switching and dependency checks

### Enhanced
- **README Quick Start Section** - Added simple startup instructions
- **Environment Variable Loading** - Backend now properly loads .env file on startup
- **Documentation Links** - Added architecture flow diagram link to README

### Fixed
- **OAuth2 Environment Variables** - Fixed issue where OAuth credentials from .env weren't being loaded
- **Backend Startup** - Environment variables now automatically loaded when using startup script

### Technical Improvements
- Automatic Java 21 environment activation in startup script
- Improved .env file validation
- Better error messages for missing configuration

## [1.0.0] - 2026-01-11

### Added
- **Authentication Methods**
  - Email/password registration with industry-standard password requirements
  - OAuth2 social login (Google, Facebook, GitHub, Twitter)
  - JWT-based authentication with access and refresh tokens
  - Automatic token refresh mechanism

- **Security Features**
  - Password validation (min 8 chars, uppercase, lowercase, digit, special char)
  - Account lockout after 5 failed login attempts (24-hour lock period)
  - Rate limiting (20 requests/minute per IP)
  - BCrypt password hashing
  - CORS protection
  - Secure cookie handling for OAuth2 flow

- **Backend (Spring Boot 3.2.1)**
  - RESTful API for authentication
  - PostgreSQL database integration
  - Multi-device session management
  - Refresh token rotation
  - Device and IP tracking
  - Comprehensive logging with configurable levels
  - Health check endpoints

- **Frontend (React 18 + TypeScript)**
  - Modern authentication UI
  - OAuth2 redirect handler
  - Zustand state management
  - Axios HTTP client with automatic token refresh
  - React Router integration

- **DevOps & Deployment**
  - Docker and Docker Compose configuration
  - Multi-stage Docker builds for optimization
  - Environment-specific configurations (dev, test, prod)
  - CI/CD pipeline with GitHub Actions
  - Automated testing and deployment
  - Health checks and monitoring

- **Documentation**
  - Comprehensive README with setup instructions
  - Deployment guide with multiple strategies
  - Architecture documentation
  - API documentation
  - Security best practices

### Technical Details
- Java 21 LTS support
- Maven build configuration with Lombok annotation processing
- Spring Security OAuth2 Client integration
- JWT token generation and validation
- PostgreSQL database schema with proper constraints
- Nginx configuration for frontend serving
- Rate limiting using Bucket4j
- Password validation using Passay library

### Configuration
- Development, test, and production profiles
- Environment variable support
- Configurable token expiration times
- Customizable CORS origins
- OAuth2 provider configuration

### Fixed
- Image URL length constraint (changed to TEXT type for unlimited length)
- Lombok annotation processing in Maven
- Vite proxy configuration for OAuth2 redirect handling
- Cookie SameSite attribute for cross-site OAuth redirects

## [Unreleased]

### Planned Features
- Password reset functionality
- Two-factor authentication (2FA)
- Account activity monitoring
- Session management dashboard
- Role-based access control (RBAC) enhancements
- API rate limiting per user
- Audit logging
- Social account linking
- HTTPS/SSL support documentation

---

## Version History

### [1.2.0] - 2026-01-11
Minor enhancement release adding email verification system for improved security and user account activation.

### [1.1.0] - 2026-01-11
Minor enhancement release with improved startup process and environment variable handling.

### [1.0.0] - 2026-01-11
Initial production release with complete authentication system.

---

## Migration Guides

### Upgrading to 1.2.0
**Email Configuration Required**: You must configure SMTP settings in your `.env` file before using email verification features.

Required environment variables:
```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM_EMAIL=noreply@yourapp.com
MAIL_FROM_NAME=Your App Name
FRONTEND_URL=http://localhost:3000
VERIFICATION_TOKEN_EXPIRATION_HOURS=24
```

**Breaking Change**: New users will now require email verification before they can access protected resources. Existing users are unaffected.

**Database Migration**: New tables will be created automatically:
- `verification_tokens` - Stores email verification tokens

### Upgrading to 1.1.0
No breaking changes. Simply update and use the new startup scripts for easier development.

Recommended: Start using `./start-backend.sh` instead of manual `mvn spring-boot:run` to ensure environment variables are loaded.

### Upgrading to 1.0.0
This is the initial release. No migration required.

---

## Contributors

- Vibhu Sinha - Initial development

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
