# Deployment Guide - Authentication Module v1.0.0

This guide provides instructions for deploying the Authentication Module across different environments.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Docker Deployment](#docker-deployment)
- [Manual Deployment](#manual-deployment)
- [CI/CD Pipeline](#cicd-pipeline)
- [Environment Variables](#environment-variables)
- [Database Migrations](#database-migrations)
- [Monitoring and Health Checks](#monitoring-and-health-checks)

## Prerequisites

### Development Environment
- Java 21 or higher
- Node.js 20 or higher
- PostgreSQL 16 or higher
- Maven 3.9+
- Docker and Docker Compose (optional)

### Production Environment
- Docker and Docker Compose
- PostgreSQL 16 (managed or containerized)
- SSL/TLS certificates
- Domain name with DNS configured

## Environment Setup

### 1. Development Environment

```bash
# Clone the repository
git clone https://github.com/your-username/authentication-module.git
cd authentication-module

# Create environment file
cp .env.example .env

# Edit .env with your credentials
nano .env
```

Required environment variables for development:
```bash
# Database
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET=your-secret-key-min-512-bits
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### 2. Test Environment

```bash
# Use test profile
export SPRING_PROFILES_ACTIVE=test

# Create test database
psql -U postgres -c "CREATE DATABASE authdb_test;"
```

### 3. Production Environment

See [Environment Variables](#environment-variables) section for complete list.

## Docker Deployment

### Quick Start with Docker Compose

```bash
# 1. Create environment file
cp .env.example .env

# 2. Configure environment variables
nano .env

# 3. Start all services
docker-compose up -d

# 4. Check logs
docker-compose logs -f

# 5. Stop services
docker-compose down
```

### Production Docker Deployment

```bash
# 1. Build images
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build

# 2. Start services
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 3. Scale backend
docker-compose up -d --scale backend=3
```

### Individual Service Deployment

```bash
# Backend only
docker build -t auth-backend:1.0.0 .
docker run -d \
  -p 8080:8080 \
  --env-file .env \
  --name auth-backend \
  auth-backend:1.0.0

# Frontend only
cd frontend
docker build -t auth-frontend:1.0.0 .
docker run -d \
  -p 3000:80 \
  --name auth-frontend \
  auth-frontend:1.0.0
```

## Manual Deployment

### Backend Deployment

```bash
# 1. Use Java 21
source ./use-java-21.sh

# 2. Build application
mvn clean package -DskipTests

# 3. Run with profile
java -jar \
  -Dspring.profiles.active=prod \
  -Dserver.port=8080 \
  target/authentication-module-1.0.0.jar
```

### Frontend Deployment

```bash
cd frontend

# 1. Install dependencies
npm ci --production

# 2. Build for production
npm run build

# 3. Serve with nginx or static server
npx serve -s dist -p 3000
```

## CI/CD Pipeline

The project uses GitHub Actions for automated CI/CD.

### Environments

- **Development** (develop branch) - Auto-deploys on push
- **Test** (main branch) - Auto-deploys after dev
- **Production** (main branch) - Requires manual approval

### Pipeline Stages

1. **Test** - Run unit and integration tests
2. **Build** - Build Docker images
3. **Push** - Push images to Docker registry
4. **Deploy** - Deploy to target environment

### GitHub Secrets Required

```
DOCKER_USERNAME
DOCKER_PASSWORD
```

### Triggering Deployment

```bash
# Deploy to development
git push origin develop

# Deploy to test/production
git push origin main
```

## Environment Variables

### Backend Environment Variables

| Variable | Dev | Test | Prod | Required | Description |
|----------|-----|------|------|----------|-------------|
| SPRING_PROFILES_ACTIVE | dev | test | prod | Yes | Active Spring profile |
| DATABASE_URL | - | - | jdbc:postgresql://... | Prod | Full database URL |
| DB_USERNAME | vibhusinha | postgres | - | Yes | Database username |
| DB_PASSWORD | - | postgres | - | Yes | Database password |
| JWT_SECRET | - | - | - | Yes | JWT signing key (min 512 bits) |
| JWT_EXPIRATION | 900000 | 900000 | 900000 | No | Access token expiry (ms) |
| JWT_REFRESH_EXPIRATION | 604800000 | 604800000 | 604800000 | No | Refresh token expiry (ms) |
| GOOGLE_CLIENT_ID | - | - | - | Yes | Google OAuth client ID |
| GOOGLE_CLIENT_SECRET | - | - | - | Yes | Google OAuth client secret |
| FRONTEND_URL | http://localhost:3000 | - | https://... | Prod | Frontend URL |
| PORT | 8080 | 8081 | 8080 | No | Backend port |

### Frontend Environment Variables

| Variable | Dev | Test | Prod | Required | Description |
|----------|-----|------|------|----------|-------------|
| VITE_API_BASE_URL | http://localhost:8080 | http://localhost:8081 | https://... | Yes | Backend API URL |
| VITE_APP_ENV | development | test | production | No | Application environment |

## Database Migrations

### Development
```bash
# Hibernate auto-updates schema
spring.jpa.hibernate.ddl-auto=update
```

### Production
```bash
# Schema validation only
spring.jpa.hibernate.ddl-auto=validate

# Use Flyway for migrations
# Add migration scripts to src/main/resources/db/migration/
```

### Manual Migration

```bash
# Create migration
psql -U postgres -d authdb < migrations/V1__initial_schema.sql

# Verify
psql -U postgres -d authdb -c "\dt"
```

## Monitoring and Health Checks

### Health Endpoints

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Frontend health
curl http://localhost:3000/health

# Docker health
docker ps
```

### Logging

```bash
# Backend logs
tail -f logs/authentication-module.log

# Docker logs
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Metrics

```bash
# Application metrics
curl http://localhost:8080/actuator/metrics

# System metrics
docker stats
```

## Rollback Procedure

### Docker Rollback

```bash
# 1. Stop current containers
docker-compose down

# 2. Pull previous image
docker pull your-username/auth-backend:previous-tag

# 3. Update docker-compose.yml with previous tag

# 4. Start containers
docker-compose up -d
```

### Manual Rollback

```bash
# 1. Stop application
pkill -f authentication-module

# 2. Deploy previous JAR
java -jar target/authentication-module-0.9.0.jar

# 3. Verify
curl http://localhost:8080/actuator/health
```

## Troubleshooting

### Common Issues

1. **Database Connection Refused**
   ```bash
   # Check PostgreSQL is running
   psql -U postgres -c "SELECT version();"
   ```

2. **OAuth2 Redirect Issues**
   - Verify redirect URIs in Google Console
   - Check CORS configuration
   - Ensure cookies are enabled

3. **Port Already in Use**
   ```bash
   # Find process
   lsof -i :8080

   # Kill process
   kill -9 <PID>
   ```

4. **Docker Build Failures**
   ```bash
   # Clean Docker cache
   docker system prune -a

   # Rebuild without cache
   docker-compose build --no-cache
   ```

## Security Checklist

- [ ] Change default JWT_SECRET
- [ ] Use strong database passwords
- [ ] Enable HTTPS in production
- [ ] Configure firewall rules
- [ ] Enable rate limiting
- [ ] Set up monitoring and alerts
- [ ] Regular security updates
- [ ] Backup database regularly

## Support

For issues and questions:
- GitHub Issues: https://github.com/your-username/authentication-module/issues
- Documentation: See README.md
