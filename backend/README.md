# VibeWrite — Complete Implementation Guide

> Spring Boot 3.3 · Java 21 · PostgreSQL 16 · Redis 7 · JWT + OAuth2

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Project Initialization](#2-project-initialization)
3. [Project Structure](#3-project-structure)
4. [Docker — Local Services](#4-docker--local-services)
5. [Dependencies (pom.xml)](#5-dependencies-pomxml)
6. [Configuration (application.yml)](#6-configuration-applicationyml)
7. [Database Migrations (Flyway)](#7-database-migrations-flyway)
8. [Step-by-Step Coding Order](#8-step-by-step-coding-order)
   - [Phase 1 — Foundation](#phase-1--foundation-days-12)
   - [Phase 2 — Security & Auth](#phase-2--security--auth-days-35)
   - [Phase 3 — Users & Profiles](#phase-3--users--profiles-days-67)
   - [Phase 4 — Articles & Content](#phase-4--articles--content-days-812)
   - [Phase 5 — Engagement](#phase-5--engagement-days-1316)
   - [Phase 6 — Feed, Search & Admin](#phase-6--feed-search--admin-days-1720)
9. [Key Code Implementations](#9-key-code-implementations)
10. [Running the Application](#10-running-the-application)
11. [Testing](#11-testing)
12. [Environment Variables Reference](#12-environment-variables-reference)
13. [Production Checklist](#13-production-checklist)

---

## 1. Prerequisites

Install these before you start:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 21 (LTS) | [adoptium.net](https://adoptium.net) — Eclipse Temurin |
| Maven | 3.9+ | Bundled in Spring Initializr project as `./mvnw` |
| Docker Desktop | Latest | [docker.com/get-started](https://www.docker.com/get-started) |
| IntelliJ IDEA / VS Code | Any | IDE of your choice |
| Git | 2.x+ | [git-scm.com](https://git-scm.com) |

Verify your Java version:

```bash
java -version
# Should print: openjdk version "21.x.x"
```

---

## 2. Project Initialization

### Step 1 — Generate the project skeleton

Go to **[start.spring.io](https://start.spring.io)** and configure:

| Setting | Value |
|---------|-------|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.3.x |
| Group | `io.vibewrite` |
| Artifact | `vibewrite` |
| Packaging | Jar |
| Java | 21 |

Add these starter dependencies from the UI:
- Spring Web
- Spring Data JPA
- Spring Security
- OAuth2 Client
- Spring Boot Actuator
- Validation
- Spring Data Redis
- Spring Mail
- Thymeleaf
- PostgreSQL Driver
- Flyway Migration
- Lombok

Click **Generate**, download the zip, and extract it:

```bash
unzip vibewrite.zip
cd vibewrite
git init
git add .
git commit -m "chore: initial Spring Initializr scaffold"
```

### Step 2 — Verify the project builds

```bash
./mvnw compile
# Should print BUILD SUCCESS
```

---

## 3. Project Structure

Create this exact package layout. Every file in this guide lives at one of these paths.

```
vibewrite/
├── pom.xml
├── docker-compose.yml
├── .env                                    ← secrets (never commit)
├── .gitignore
└── src/
    ├── main/
    │   ├── java/io/vibewrite/
    │   │   ├── VibeWriteApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── JwtConfig.java
    │   │   │   ├── RedisConfig.java
    │   │   │   ├── S3Config.java
    │   │   │   ├── OpenApiConfig.java
    │   │   │   └── MailConfig.java
    │   │   │
    │   │   ├── domain/
    │   │   │   ├── user/
    │   │   │   │   ├── User.java                    ← Spring Security principal
    │   │   │   │   ├── UserProfile.java             ← display data (separate table)
    │   │   │   │   ├── OAuthIdentity.java
    │   │   │   │   ├── RefreshToken.java
    │   │   │   │   ├── PasswordResetToken.java
    │   │   │   │   ├── EmailVerificationToken.java
    │   │   │   │   ├── UserRole.java                ← enum: USER, ADMIN
    │   │   │   │   └── UserStatus.java              ← enum: ACTIVE, DISABLED
    │   │   │   ├── article/
    │   │   │   │   ├── Article.java
    │   │   │   │   ├── ArticleTag.java
    │   │   │   │   ├── Category.java
    │   │   │   │   ├── Tag.java
    │   │   │   │   └── ArticleStatus.java           ← enum: DRAFT, PUBLISHED, SCHEDULED
    │   │   │   ├── engagement/
    │   │   │   │   ├── Comment.java
    │   │   │   │   ├── ArticleClap.java
    │   │   │   │   ├── CommentLike.java
    │   │   │   │   ├── Bookmark.java
    │   │   │   │   ├── UserFollow.java
    │   │   │   │   ├── TagFollow.java
    │   │   │   │   └── ArticleView.java
    │   │   │   └── notification/
    │   │   │       ├── Notification.java
    │   │   │       ├── NotificationType.java        ← enum
    │   │   │       └── TargetType.java              ← enum
    │   │   │
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   ├── UserProfileRepository.java
    │   │   │   ├── ArticleRepository.java
    │   │   │   ├── CommentRepository.java
    │   │   │   ├── BookmarkRepository.java
    │   │   │   ├── ArticleClapRepository.java
    │   │   │   ├── UserFollowRepository.java
    │   │   │   ├── TagFollowRepository.java
    │   │   │   ├── NotificationRepository.java
    │   │   │   └── ArticleViewRepository.java
    │   │   │
    │   │   ├── service/
    │   │   │   ├── auth/
    │   │   │   │   ├── AuthService.java
    │   │   │   │   ├── JwtService.java
    │   │   │   │   ├── OAuth2Service.java
    │   │   │   │   └── TokenCleanupScheduler.java
    │   │   │   ├── user/
    │   │   │   │   └── UserService.java
    │   │   │   ├── article/
    │   │   │   │   ├── ArticleService.java
    │   │   │   │   └── ScheduledPublishingService.java
    │   │   │   ├── engagement/
    │   │   │   │   ├── CommentService.java
    │   │   │   │   ├── ClapService.java
    │   │   │   │   ├── BookmarkService.java
    │   │   │   │   └── FollowService.java
    │   │   │   ├── feed/
    │   │   │   │   └── FeedService.java
    │   │   │   ├── search/
    │   │   │   │   └── SearchService.java
    │   │   │   ├── upload/
    │   │   │   │   └── ImageUploadService.java
    │   │   │   ├── notification/
    │   │   │   │   └── NotificationService.java
    │   │   │   └── mail/
    │   │   │       └── MailService.java
    │   │   │
    │   │   ├── web/
    │   │   │   ├── dto/
    │   │   │   │   ├── request/                     ← one per endpoint
    │   │   │   │   │   ├── RegisterRequest.java
    │   │   │   │   │   ├── LoginRequest.java
    │   │   │   │   │   ├── CreateArticleRequest.java
    │   │   │   │   │   └── ...
    │   │   │   │   └── response/                    ← outbound JSON shapes
    │   │   │   │       ├── ApiResponse.java
    │   │   │   │       ├── PagedResponse.java
    │   │   │   │       ├── UserMeResponse.java
    │   │   │   │       ├── ArticleResponse.java
    │   │   │   │       └── ...
    │   │   │   ├── controller/
    │   │   │   │   ├── AuthController.java
    │   │   │   │   ├── UserController.java
    │   │   │   │   ├── ArticleController.java
    │   │   │   │   ├── CommentController.java
    │   │   │   │   ├── FeedController.java
    │   │   │   │   ├── EngagementController.java
    │   │   │   │   ├── DashboardController.java
    │   │   │   │   ├── SearchController.java
    │   │   │   │   ├── UploadController.java
    │   │   │   │   └── AdminController.java
    │   │   │   └── mapper/
    │   │   │       ├── UserMapper.java
    │   │   │       └── ArticleMapper.java
    │   │   │
    │   │   ├── security/
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   ├── CustomUserDetailsService.java
    │   │   │   └── OAuth2SuccessHandler.java
    │   │   │
    │   │   └── exception/
    │   │       ├── GlobalExceptionHandler.java
    │   │       ├── ResourceNotFoundException.java
    │   │       ├── ConflictException.java
    │   │       └── UnprocessableEntityException.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── db/migration/
    │       │   ├── V1__create_users.sql
    │       │   ├── V2__create_user_profiles.sql
    │       │   ├── V3__create_articles.sql
    │       │   └── V4__create_engagement.sql
    │       └── templates/mail/
    │           ├── verify-email.html
    │           └── reset-password.html
    │
    └── test/
        └── java/io/vibewrite/
            ├── service/                             ← unit tests (Mockito)
            ├── web/                                 ← @WebMvcTest slice tests
            └── integration/                         ← @SpringBootTest + Testcontainers
```

---

## 4. Docker — Local Services

Create `docker-compose.yml` in the project root:

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: vibewrite-postgres
    environment:
      POSTGRES_DB: vibewrite
      POSTGRES_USER: vibewrite
      POSTGRES_PASSWORD: vibewrite
    ports:
      - '5432:5432'
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ['CMD-SHELL', 'pg_isready -U vibewrite']
      interval: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: vibewrite-redis
    ports:
      - '6379:6379'
    command: redis-server --save 60 1 --loglevel warning
    volumes:
      - redisdata:/data

  minio:
    image: minio/minio:latest
    container_name: vibewrite-minio
    ports:
      - '9000:9000'
      - '9001:9001'
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ':9001'
    volumes:
      - miniodata:/data

  mailhog:
    image: mailhog/mailhog:latest
    container_name: vibewrite-mail
    ports:
      - '1025:1025'    # SMTP — point Spring Mail here
      - '8025:8025'    # Web UI — view emails in browser

volumes:
  pgdata:
  redisdata:
  miniodata:
```

Start everything:

```bash
docker-compose up -d

# Verify all 4 containers are running
docker-compose ps
```

Access points:
- **Postgres** → `localhost:5432`
- **Redis** → `localhost:6379`
- **MinIO console** → [http://localhost:9001](http://localhost:9001) (user: `minioadmin`, pass: `minioadmin`)
- **Mailhog UI** → [http://localhost:8025](http://localhost:8025)

---

## 5. Dependencies (pom.xml)

Replace the `<dependencies>` block in your generated `pom.xml` with this complete list. Also add the `<dependencyManagement>` and `<build>` sections shown.

```xml
<properties>
    <java.version>21</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <jjwt.version>0.12.6</jjwt.version>
</properties>

<dependencies>

    <!-- ── Core Web ─────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- ── Security + JWT ───────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>

    <!-- ── Database ──────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- ── Redis ─────────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- ── Email ─────────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <!-- ── AWS S3 / MinIO ─────────────────────────────── -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>2.26.7</version>
    </dependency>

    <!-- ── Utilities ──────────────────────────────────── -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
    <dependency>
        <groupId>com.github.slugify</groupId>
        <artifactId>slugify</artifactId>
        <version>3.0.6</version>
    </dependency>

    <!-- ── Monitoring ────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- ── API Docs ───────────────────────────────────── -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.5.0</version>
    </dependency>

    <!-- ── Testing ───────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
        <!-- MapStruct + Lombok must be processed in this order -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 6. Configuration (application.yml)

Create `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: vibewrite

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/vibewrite}
    username: ${DB_USER:vibewrite}
    password: ${DB_PASS:vibewrite}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate          # Flyway owns the schema — NEVER use create/update
    show-sql: false
    open-in-view: false           # Disable OSIV to avoid N+1 traps
    properties:
      hibernate:
        format_sql: true
        default_schema: public
        jdbc:
          time_zone: UTC

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASS:}
      timeout: 2000ms

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, profile, email
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email

  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
    username: ${MAIL_USER:test}
    password: ${MAIL_PASS:test}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.connectiontimeout: 5000
      mail.smtp.timeout: 5000

  servlet:
    multipart:
      max-file-size: 15MB
      max-request-size: 15MB

app:
  jwt:
    secret: ${JWT_SECRET}             # base64, min 64 chars
    access-token-expiry: 900          # 15 minutes
    refresh-token-expiry: 604800      # 7 days
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
  cdn-base-url: ${CDN_BASE_URL:http://localhost:8080}
  rate-limit:
    default: 100                      # requests/min per user
    search: 30
    auth: 10

aws:
  s3:
    bucket: ${S3_BUCKET:vibewrite-media}
    region: ${AWS_REGION:us-east-1}
    access-key: ${AWS_ACCESS_KEY}
    secret-key: ${AWS_SECRET_KEY}
    endpoint: ${S3_ENDPOINT:}         # Set for MinIO: http://localhost:9000

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: method

management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
  endpoint:
    health:
      show-details: when-authorized
```

Create `src/main/resources/application-dev.yml` for dev overrides:

```yaml
spring:
  jpa:
    show-sql: true

logging:
  level:
    io.vibewrite: DEBUG
    org.springframework.security: DEBUG
```

---

## 7. Database Migrations (Flyway)

Place these files in `src/main/resources/db/migration/`. Flyway runs them in version order on every startup.

### V1__create_users.sql

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TYPE user_role   AS ENUM ('USER', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'DISABLED');

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    role            user_role    NOT NULL DEFAULT 'USER',
    status          user_status  NOT NULL DEFAULT 'ACTIVE',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE oauth_identities (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    access_token     TEXT,
    refresh_token    TEXT,
    token_expires_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    is_used     BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    is_used     BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_users_email         ON users(email);
```

### V2__create_user_profiles.sql

```sql
CREATE TABLE user_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    username        VARCHAR(30)  NOT NULL UNIQUE,
    full_name       VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    bio             VARCHAR(300),
    website         VARCHAR(200),
    twitter_handle  VARCHAR(50),
    linkedin_url    VARCHAR(200),
    followers_count INT NOT NULL DEFAULT 0,
    following_count INT NOT NULL DEFAULT 0,
    articles_count  INT NOT NULL DEFAULT 0,
    last_active_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Fast username lookup (public profile URL, follow/unfollow by username)
CREATE INDEX idx_user_profiles_username ON user_profiles(username);

-- Trigram index for admin user search by name
CREATE INDEX idx_user_profiles_fullname_trgm
    ON user_profiles USING GIN (full_name gin_trgm_ops);
```

### V3__create_articles.sql

```sql
CREATE TYPE article_status AS ENUM ('DRAFT', 'PUBLISHED', 'SCHEDULED');

CREATE TABLE categories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(50) NOT NULL UNIQUE,
    slug          VARCHAR(60) NOT NULL UNIQUE,
    article_count INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tags (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(30) NOT NULL UNIQUE,
    slug           VARCHAR(40) NOT NULL UNIQUE,
    article_count  INT NOT NULL DEFAULT 0,
    follower_count INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE articles (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id          UUID REFERENCES categories(id) ON DELETE SET NULL,
    title                VARCHAR(200) NOT NULL,
    slug                 VARCHAR(220) NOT NULL UNIQUE,
    content              TEXT,
    excerpt              VARCHAR(300),
    featured_image_url   VARCHAR(500),
    reading_time_minutes INT NOT NULL DEFAULT 1,
    status               article_status NOT NULL DEFAULT 'DRAFT',
    is_featured          BOOLEAN NOT NULL DEFAULT FALSE,
    view_count           INT NOT NULL DEFAULT 0,
    clap_count           INT NOT NULL DEFAULT 0,
    comment_count        INT NOT NULL DEFAULT 0,
    bookmark_count       INT NOT NULL DEFAULT 0,
    meta_title           VARCHAR(70),
    meta_description     VARCHAR(160),
    canonical_url        VARCHAR(500),
    scheduled_at         TIMESTAMPTZ,
    published_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE article_tags (
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_id)
);

CREATE TABLE uploaded_images (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uploader_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_url   VARCHAR(500) NOT NULL,
    thumbnail_url  VARCHAR(500),
    webp_url       VARCHAR(500),
    width          INT,
    height         INT,
    file_size_bytes INT,
    mime_type      VARCHAR(50),
    is_attached    BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_articles_author    ON articles(author_id);
CREATE INDEX idx_articles_status    ON articles(status);
CREATE INDEX idx_articles_published ON articles(published_at DESC)
    WHERE status = 'PUBLISHED';
CREATE INDEX idx_articles_scheduled ON articles(scheduled_at)
    WHERE status = 'SCHEDULED';

-- Full-text search using PostgreSQL GIN
CREATE INDEX idx_articles_fts ON articles
    USING GIN(to_tsvector('english', title || ' ' || COALESCE(content, '')));
```

### V4__create_engagement.sql

```sql
CREATE TABLE comments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id        UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    author_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    content           TEXT NOT NULL,
    like_count        INT NOT NULL DEFAULT 0,
    is_edited         BOOLEAN NOT NULL DEFAULT FALSE,
    status            VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE article_claps (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id  UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    clap_count  INT NOT NULL DEFAULT 0
        CONSTRAINT clap_count_range CHECK (clap_count BETWEEN 1 AND 50),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, article_id)
);

CREATE TABLE comment_likes (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, comment_id)
);

CREATE TABLE bookmarks (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, article_id)
);

CREATE TABLE user_follows (
    follower_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id),
    CONSTRAINT no_self_follow CHECK (follower_id <> following_id)
);

CREATE TABLE tag_follows (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, tag_id)
);

CREATE TABLE article_views (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id        UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    user_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    ip_hash           VARCHAR(64),
    read_time_seconds INT,
    read_through      BOOLEAN NOT NULL DEFAULT FALSE,
    viewed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    type         VARCHAR(30) NOT NULL,
    target_type  VARCHAR(20) NOT NULL,
    target_id    UUID,
    is_read      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Performance indexes
CREATE INDEX idx_comments_article     ON comments(article_id);
CREATE INDEX idx_comments_parent      ON comments(parent_comment_id)
    WHERE parent_comment_id IS NOT NULL;
CREATE INDEX idx_article_views_article ON article_views(article_id);
CREATE INDEX idx_article_views_dedup  ON article_views(article_id, ip_hash, viewed_at);
CREATE INDEX idx_notifications_inbox  ON notifications(recipient_id, is_read, created_at DESC);
CREATE INDEX idx_user_follows_follower ON user_follows(follower_id);
CREATE INDEX idx_user_follows_following ON user_follows(following_id);
```

---

## 8. Step-by-Step Coding Order

> **Rule:** Build each phase completely before starting the next. Each phase depends on the previous.

---

### Phase 1 — Foundation (Days 1–2)

Build the skeleton that everything else plugs into.

#### Step 1.1 — Entry point

```java
// src/main/java/io/vibewrite/VibeWriteApplication.java
@SpringBootApplication
@EnableScheduling
public class VibeWriteApplication {
    public static void main(String[] args) {
        SpringApplication.run(VibeWriteApplication.class, args);
    }
}
```

#### Step 1.2 — Generic API response wrappers

```java
// web/dto/response/ApiResponse.java
@Data @Builder
public class ApiResponse<T> {
    private boolean success;
    private T       data;
    private String  message;
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }
    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder().success(true).data(data).message(message).build();
    }
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(201)
            .body(ApiResponse.<T>builder().success(true).data(data).message(message).build());
    }
}
```

#### Step 1.3 — Custom exceptions

```java
// exception/ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

// exception/ConflictException.java
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}

// exception/UnprocessableEntityException.java
public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String message) { super(message); }
}
```

#### Step 1.4 — Global exception handler

```java
// exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return errorResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException ex) {
        return errorResponse(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<?> handleUnprocessable(UnprocessableEntityException ex) {
        return errorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> Map.of("field", e.getField(), "message", e.getDefaultMessage()))
            .toList();
        // return validation error response with details list
    }

    private ResponseEntity<?> errorResponse(HttpStatus status, String code, String message) {
        var body = Map.of(
            "success", false,
            "error", Map.of("code", code, "message", message),
            "timestamp", Instant.now()
        );
        return ResponseEntity.status(status).body(body);
    }
}
```

#### Step 1.5 — All enums

```java
// domain/user/UserRole.java
public enum UserRole { USER, ADMIN }

// domain/user/UserStatus.java
public enum UserStatus { ACTIVE, DISABLED }

// domain/article/ArticleStatus.java
public enum ArticleStatus { DRAFT, PUBLISHED, SCHEDULED }

// domain/notification/NotificationType.java
public enum NotificationType {
    NEW_COMMENT, NEW_REPLY, NEW_FOLLOWER, ARTICLE_PUBLISHED, ARTICLE_FEATURED
}

// domain/notification/TargetType.java
public enum TargetType { ARTICLE, COMMENT, USER }
```

#### Step 1.6 — User entity (Spring Security principal)

```java
// domain/user/User.java
@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private boolean emailVerified = false;

    private Instant lastLoginAt;

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp   private Instant updatedAt;

    // Lazy — only loaded when display data is needed
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserProfile profile;

    // ── UserDetails ───────────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String  getPassword()             { return passwordHash; }
    @Override public String  getUsername()             { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return status == UserStatus.ACTIVE; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return status == UserStatus.ACTIVE; }
}
```

#### Step 1.7 — UserProfile entity

```java
// domain/user/UserProfile.java
@Entity
@Table(name = "user_profiles")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true, nullable = false, length = 30)
    private String username;

    @Column(nullable = false, length = 100)
    private String fullName;

    private String avatarUrl;

    @Column(length = 300)
    private String bio;

    private String website;
    private String twitterHandle;
    private String linkedInUrl;

    @Column(nullable = false) private int followersCount = 0;
    @Column(nullable = false) private int followingCount = 0;
    @Column(nullable = false) private int articlesCount  = 0;

    private Instant lastActiveAt;

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp   private Instant updatedAt;
}
```

#### Step 1.8 — Repositories

```java
// repository/UserRepository.java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

// repository/UserProfileRepository.java
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findByUserId(UUID userId);
    boolean existsByUsername(String username);
}
```

#### Checkpoint 1 ✓

```bash
./mvnw spring-boot:run
# App should start, connect to Postgres, and run V1+V2 Flyway migrations
# Visit http://localhost:8080/actuator/health — should return {"status":"UP"}
```

---

### Phase 2 — Security & Auth (Days 3–5)

#### Step 2.1 — JwtService

```java
// service/auth/JwtService.java
@Service
public class JwtService {

    @Value("${app.jwt.secret}")       private String secret;
    @Value("${app.jwt.access-token-expiry}") private long accessExpiry;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role",  user.getRole())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpiry * 1000))
            .signWith(key())
            .compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
            .verifyWith(key())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateAndExtract(token).getSubject());
    }
}
```

#### Step 2.2 — CustomUserDetailsService

```java
// security/CustomUserDetailsService.java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public UserDetails loadUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    }
}
```

#### Step 2.3 — JwtAuthenticationFilter

```java
// security/JwtAuthenticationFilter.java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        try {
            String token = header.substring(7);
            UUID userId = jwtService.extractUserId(token);
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserById(userId);
                var auth = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ignored) { /* invalid token — just continue unauthenticated */ }
        chain.doFilter(req, res);
    }
}
```

#### Step 2.4 — SecurityConfig

```java
// config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final OAuth2SuccessHandler    oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/auth/**").permitAll()
                // Public reads
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/feed").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tags/**").permitAll()
                // Admin only
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Docs
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(o -> o.successHandler(oauth2SuccessHandler))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

#### Step 2.5 — AuthService

```java
// service/auth/AuthService.java
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository             userRepository;
    private final UserProfileRepository      profileRepository;
    private final PasswordEncoder            encoder;
    private final JwtService                 jwtService;
    private final AuthenticationManager      authManager;

    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new ConflictException("Email already registered");
        if (profileRepository.existsByUsername(req.getUsername()))
            throw new ConflictException("Username already taken");

        // Insert into users
        User user = User.builder()
            .email(req.getEmail())
            .passwordHash(encoder.encode(req.getPassword()))
            .build();
        userRepository.save(user);

        // Insert into user_profiles — same transaction
        UserProfile profile = UserProfile.builder()
            .user(user)
            .username(req.getUsername())
            .fullName(req.getFullName())
            .build();
        profileRepository.save(profile);

        return user;
    }

    public LoginResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
        user.setLastLoginAt(Instant.now());
        String token = jwtService.generateAccessToken(user);
        // also create and save refresh token...
        return LoginResponse.from(user, token);
    }
}
```

#### Step 2.6 — AuthController

```java
// web/controller/AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        User user = authService.register(req);
        return ApiResponse.created(RegisterResponse.from(user), 
            "Registration successful. Please check your email to verify your account.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletResponse response) {
        LoginResponse result = authService.login(req);
        // Set refresh token as HttpOnly cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", result.getRefreshToken())
            .httpOnly(true).secure(true).sameSite("Strict")
            .maxAge(604800).path("/api/v1/auth/refresh").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal User user, ...) {
        authService.logout(user);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }
    
    // ... other auth endpoints
}
```

#### Checkpoint 2 ✓

```bash
# Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","email":"test@example.com","password":"Test1234!","fullName":"Test User"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"Test1234!"}'
# Copy the accessToken from the response
```

---

### Phase 3 — Users & Profiles (Days 6–7)

1. **Create `UserService.java`** — `getMe(User user)`, `getByUsername(String username)`, `updateProfile(User user, UpdateProfileRequest req)`, `changeUsername(User user, String newUsername)`
2. **Create `UserController.java`** — wire `GET /users/me`, `GET /users/me/profile`, `GET /users/{username}`, `PUT /users/me`, `PUT /users/me/username`
3. **Create `S3Config.java`** — configure `S3Client` bean pointing at MinIO for local dev
4. **Create `ImageUploadService.java`** — multipart to S3, generate thumbnail URL
5. **Wire `MailService.java`** — `sendVerificationEmail()`, `sendPasswordResetEmail()` using Thymeleaf templates
6. **Add `POST /users/me/avatar`** and `DELETE /users/me/avatar` to `UserController`

#### Checkpoint 3 ✓

```bash
TOKEN="your-access-token-here"

# Get own profile
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"

# Get public profile
curl http://localhost:8080/api/v1/users/testuser
```

---

### Phase 4 — Articles & Content (Days 8–12)

1. **Create remaining entities** — `Article`, `Category`, `Tag`, `ArticleTag`, `UploadedImage`

2. **Create `ArticleRepository`** with custom queries:
   ```java
   Optional<Article> findBySlugAndStatus(String slug, ArticleStatus status);
   Page<Article> findByAuthorIdAndStatus(UUID authorId, ArticleStatus status, Pageable pageable);
   boolean existsBySlug(String slug);
   
   @Query("SELECT a FROM Article a WHERE to_tsvector('english', a.title || ' ' || a.content) " +
          "@@ plainto_tsquery('english', :query) AND a.status = 'PUBLISHED'")
   Page<Article> fullTextSearch(@Param("query") String query, Pageable pageable);
   ```

3. **Create `ArticleService.java`** with these core methods:

   ```java
   // Slug generation — unique, stable after publish
   private String generateUniqueSlug(String title) {
       String base = slugify.slugify(title);
       String slug = base;
       int counter = 2;
       while (articleRepository.existsBySlug(slug)) {
           slug = base + "-" + counter++;
       }
       return slug;
   }

   // Reading time — strips HTML, 200 wpm
   private int calculateReadingTime(String html) {
       String text = html.replaceAll("<[^>]+>", " ");
       long words = Arrays.stream(text.split("\\s+"))
           .filter(w -> !w.isBlank()).count();
       return (int) Math.max(1, Math.ceil(words / 200.0));
   }

   @Transactional
   public Article publish(UUID articleId, User author) {
       Article article = getOwnedArticle(articleId, author);
       if (!author.isEmailVerified())
           throw new UnprocessableEntityException("Email must be verified before publishing");
       if (article.getStatus() == ArticleStatus.PUBLISHED)
           throw new UnprocessableEntityException("Article is already published");
       article.setStatus(ArticleStatus.PUBLISHED);
       article.setPublishedAt(Instant.now());
       // increment user_profiles.articles_count
       // trigger notifications to followers
       return articleRepository.save(article);
   }
   ```

4. **Create `ArticleController.java`** — all 12 article endpoints

5. **Create `ScheduledPublishingService.java`**:
   ```java
   @Scheduled(fixedDelay = 60_000)   // every minute
   @Transactional
   public void publishScheduledArticles() {
       List<Article> due = articleRepository.findByStatusAndScheduledAtBefore(
           ArticleStatus.SCHEDULED, Instant.now());
       due.forEach(a -> {
           a.setStatus(ArticleStatus.PUBLISHED);
           a.setPublishedAt(Instant.now());
       });
       articleRepository.saveAll(due);
   }
   ```

6. **Create `FeedService.java`** and `FeedController.java`
7. **Create `SearchService.java`** and `SearchController.java`

#### Checkpoint 4 ✓

```bash
TOKEN="your-access-token"

# Create draft
curl -X POST http://localhost:8080/api/v1/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"My First Post","content":"<p>Hello world</p>"}'

# Publish it (replace ARTICLE_ID)
curl -X POST http://localhost:8080/api/v1/articles/ARTICLE_ID/publish \
  -H "Authorization: Bearer $TOKEN"

# Read publicly
curl http://localhost:8080/api/v1/articles/my-first-post
```

---

### Phase 5 — Engagement (Days 13–16)

1. **Create entities** — `Comment`, `ArticleClap`, `CommentLike`, `Bookmark`, `UserFollow`, `TagFollow`, `ArticleView`, `Notification`

2. **Create `CommentService.java`**:
   - `postComment()` — inserts comment, increments `articles.comment_count`, triggers notification
   - `editComment()` — enforces 15-minute edit window: `if (Duration.between(comment.getCreatedAt(), Instant.now()).toMinutes() > 15) throw ...`
   - `deleteComment()` — soft delete: sets content to `[comment removed]`, status to `REMOVED`

3. **Create `ClapService.java`**:
   ```java
   @Transactional
   public ClapResponse clap(UUID articleId, User user, int count) {
       if (article.getAuthorId().equals(user.getId()))
           throw new UnprocessableEntityException("Cannot clap for your own article");
       
       ArticleClap clap = clapRepo.findByUserIdAndArticleId(user.getId(), articleId)
           .orElse(ArticleClap.builder().userId(user.getId()).articleId(articleId).clapCount(0).build());
       
       int newTotal = clap.getClapCount() + count;
       if (newTotal > 50)
           throw new UnprocessableEntityException("Maximum 50 claps per article reached");
       
       clap.setClapCount(newTotal);
       clapRepo.save(clap);
       
       // Update denormalized counter
       articleRepo.incrementClapCount(articleId, count);
       return new ClapResponse(newTotal, 50 - newTotal);
   }
   ```

4. **Create `BookmarkService.java`** — toggle logic with counter update
5. **Create `FollowService.java`** — insert/delete `user_follows`, update both `followers_count` and `following_count` in `user_profiles`
6. **Create `NotificationService.java`** — `createNotification(recipient, actor, type, targetType, targetId)`
7. **Create `CommentController.java`** and **`EngagementController.java`**

#### Checkpoint 5 ✓

```bash
TOKEN="your-token"
SLUG="my-first-post"

# Clap for article
curl -X POST http://localhost:8080/api/v1/articles/$SLUG/clap \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"count":5}'

# Post a comment
curl -X POST http://localhost:8080/api/v1/articles/$SLUG/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"content":"Great article!"}'

# Bookmark it
curl -X POST http://localhost:8080/api/v1/articles/$SLUG/bookmark \
  -H "Authorization: Bearer $TOKEN"
```

---

### Phase 6 — Feed, Search & Admin (Days 17–20)

1. **Create `DashboardController.java`** — summary stats, article list, per-article analytics

2. **Create `AdminController.java`** — annotate every method with `@PreAuthorize("hasRole('ADMIN')")`

3. **Add Redis rate limiting** via `HandlerInterceptor`:
   ```java
   @Component
   public class RateLimitInterceptor implements HandlerInterceptor {
       private final RedisTemplate<String, Integer> redisTemplate;

       @Override
       public boolean preHandle(HttpServletRequest req, ...) {
           String key = "rl:" + getClientIp(req) + ":" + req.getRequestURI();
           Integer count = redisTemplate.opsForValue().get(key);
           if (count != null && count >= getLimit(req.getRequestURI())) {
               response.setStatus(429);
               return false;
           }
           redisTemplate.opsForValue().increment(key);
           redisTemplate.expire(key, 1, TimeUnit.MINUTES);
           return true;
       }
   }
   ```

4. **Add OpenAPI annotations** to all controllers:
   ```java
   @Tag(name = "Articles", description = "Article CRUD and publishing")
   @Operation(summary = "Get article by slug", description = "Returns a published article...")
   ```

5. **Write unit tests** for `AuthService`, `ArticleService`, `ClapService`

6. **Write integration tests** using Testcontainers:
   ```java
   @SpringBootTest
   @Testcontainers
   class ArticleIntegrationTest {
       @Container
       static PostgreSQLContainer<?> postgres =
           new PostgreSQLContainer<>("postgres:16-alpine");
   
       @DynamicPropertySource
       static void props(DynamicPropertyRegistry r) {
           r.add("spring.datasource.url", postgres::getJdbcUrl);
           r.add("spring.datasource.username", postgres::getUsername);
           r.add("spring.datasource.password", postgres::getPassword);
       }
   
       @Test
       void registerAndLoginFlow() { ... }
   }
   ```

---

## 9. Key Code Implementations

### Slug generation (unique + stable)

```java
private String generateUniqueSlug(String title) {
    String base      = slugify.slugify(title);   // "My First Post!" → "my-first-post"
    String candidate = base;
    int    counter   = 2;
    while (articleRepository.existsBySlug(candidate)) {
        candidate = base + "-" + counter++;       // "my-first-post-2", "-3", ...
    }
    return candidate;
}
```

> **Important:** Never re-generate the slug for a PUBLISHED article — it would break existing URLs. Only regenerate for DRAFT status.

### Reading time calculation

```java
private int calculateReadingTime(String htmlContent) {
    // Strip all HTML tags
    String text = htmlContent.replaceAll("<[^>]+>", " ");
    // Remove extra whitespace and count words
    long wordCount = Arrays.stream(text.trim().split("\\s+"))
        .filter(w -> !w.isBlank())
        .count();
    // Average adult reading speed = 200 words/min, minimum 1 minute
    return (int) Math.max(1, Math.ceil(wordCount / 200.0));
}
```

### Denormalized counter updates

Never use `COUNT(*)` in hot paths. Update counters atomically:

```java
// ArticleRepository
@Modifying
@Query("UPDATE Article a SET a.clapCount = a.clapCount + :delta WHERE a.id = :id")
void incrementClapCount(@Param("id") UUID id, @Param("delta") int delta);

@Modifying
@Query("UPDATE Article a SET a.commentCount = a.commentCount + 1 WHERE a.id = :id")
void incrementCommentCount(@Param("id") UUID id);

// UserProfileRepository
@Modifying
@Query("UPDATE UserProfile p SET p.followersCount = p.followersCount + :delta WHERE p.userId = :userId")
void updateFollowersCount(@Param("userId") UUID userId, @Param("delta") int delta);
```

### Article view deduplication

Prevent the same visitor from inflating view counts:

```java
public void recordView(UUID articleId, User user, String ipAddress) {
    String ipHash = DigestUtils.sha256Hex(ipAddress);
    Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
    
    // Check if this IP already viewed in the last 24 hours
    boolean alreadyViewed = viewRepository
        .existsByArticleIdAndIpHashAndViewedAtAfter(articleId, ipHash, cutoff);
    
    if (!alreadyViewed) {
        articleRepository.incrementViewCount(articleId);
        viewRepository.save(ArticleView.builder()
            .articleId(articleId)
            .userId(user != null ? user.getId() : null)
            .ipHash(ipHash)
            .build());
    }
}
```

---

## 10. Running the Application

### Create `.env` file (project root — never commit)

```bash
DB_URL=jdbc:postgresql://localhost:5432/vibewrite
DB_USER=vibewrite
DB_PASS=vibewrite
REDIS_HOST=localhost
REDIS_PORT=6379

# Generate with: openssl rand -base64 64
JWT_SECRET=bXlzdXBlcnNlY3JldGtleXRoYXRpczY0Y2hhcnNsb25nZm9yand0c2lnbmluZw==

MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USER=test
MAIL_PASS=test

GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

AWS_ACCESS_KEY=minioadmin
AWS_SECRET_KEY=minioadmin
S3_BUCKET=vibewrite-media
S3_ENDPOINT=http://localhost:9000
AWS_REGION=us-east-1

FRONTEND_URL=http://localhost:3000
```

Add `.env` to `.gitignore`:

```
.env
*.env
```

### Start the application

```bash
# 1. Start backing services
docker-compose up -d

# 2. Run with dev profile (loads application-dev.yml overrides)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or load .env manually
export $(cat .env | xargs) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Verify it's running

| URL | Expected |
|-----|----------|
| http://localhost:8080/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8025 | Mailhog inbox |
| http://localhost:9001 | MinIO console |

---

## 11. Testing

### Run all tests

```bash
./mvnw test
```

### Run a single test class

```bash
./mvnw test -Dtest=AuthServiceTest
```

### Run tests with coverage report

```bash
./mvnw verify
# Report at: target/site/jacoco/index.html
```

### Unit test example (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository          userRepository;
    @Mock UserProfileRepository   profileRepository;
    @Mock PasswordEncoder         encoder;
    @InjectMocks AuthService      authService;

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertThrows(ConflictException.class,
            () -> authService.register(new RegisterRequest("user", "test@example.com", "Pass1!", "Test")));
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(profileRepository.existsByUsername("john_doe")).thenReturn(true);
        assertThrows(ConflictException.class,
            () -> authService.register(new RegisterRequest("john_doe", "new@example.com", "Pass1!", "John")));
    }
}
```

### Integration test example (Testcontainers)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vibewrite")
            .withUsername("vibewrite")
            .withPassword("vibewrite");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired TestRestTemplate restTemplate;

    @Test
    void fullRegisterLoginFlow() {
        var req = Map.of(
            "username", "integuser",
            "email",    "integ@example.com",
            "password", "Test1234!",
            "fullName", "Integ User"
        );
        var regRes = restTemplate.postForEntity("/api/v1/auth/register", req, String.class);
        assertThat(regRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var loginReq = Map.of("email", "integ@example.com", "password", "Test1234!");
        var loginRes = restTemplate.postForEntity("/api/v1/auth/login", loginReq, String.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginRes.getBody()).contains("accessToken");
    }
}
```

---

## 12. Environment Variables Reference

| Variable              | Required       | Description                                            |
|-----------------------|----------------|--------------------------------------------------------|
| `DB_URL`              | Yes            | PostgreSQL JDBC URL                                    |
| `DB_USER`             | Yes            | Database username                                      |
| `DB_PASS`             | Yes            | Database password                                      |
| `REDIS_HOST`          | Yes            | Redis hostname                                         |
| `REDIS_PORT`          | No (def. 6379) | Redis port                                             |
| `JWT_SECRET`          | Yes            | Base64-encoded secret, **minimum 64 characters**       |
| `GOOGLE_CLIENT_ID`    | OAuth2         | From Google Cloud Console → Credentials                |
| `GOOGLE_CLIENT_SECRET`| OAuth2         | From Google Cloud Console → Credentials                |
| `GITHUB_CLIENT_ID`    | OAuth2         | From GitHub → Settings → Developer Settings           |
| `GITHUB_CLIENT_SECRET`| OAuth2         | From GitHub → Settings → Developer Settings           |
| `MAIL_HOST`           | Yes            | SMTP server (use `localhost` for Mailhog dev)          |
| `MAIL_PORT`           | No (def. 1025) | SMTP port                                              |
| `MAIL_USER`           | Yes            | SMTP username                                          |
| `MAIL_PASS`           | Yes            | SMTP password                                          |
| `AWS_ACCESS_KEY`      | Uploads        | S3 or MinIO access key                                 |
| `AWS_SECRET_KEY`      | Uploads        | S3 or MinIO secret key                                 |
| `S3_BUCKET`           | Uploads        | Bucket name for media storage                          |
| `S3_ENDPOINT`         | MinIO only     | Custom endpoint URL (`http://localhost:9000`)           |
| `AWS_REGION`          | No (def. us-east-1) | AWS region or any value for MinIO                |
| `FRONTEND_URL`        | Yes            | Frontend origin for CORS and OAuth2 redirect           |
| `CDN_BASE_URL`        | No             | Base URL for CDN-served media                          |

---

## 13. Production Checklist

Before deploying to production, verify every item:

| # | Item | Notes |
|---|------|-------|
| 1 | All secrets in environment variables | Never in `application.yml` — use a vault |
| 2 | `JWT_SECRET` is at least 64 chars | `openssl rand -base64 64` |
| 3 | `BCryptPasswordEncoder` strength = 12 | Already set in `SecurityConfig` |
| 4 | Flyway migrations tested on clean DB drop | Drop and recreate locally before every deploy |
| 5 | `spring.jpa.ddl-auto = validate` | NEVER `create` or `update` in prod |
| 6 | `spring.jpa.open-in-view = false` | Prevents N+1 LazyInit exceptions in prod |
| 7 | CORS origins locked to frontend domain | `SecurityConfig.corsConfigurationSource()` |
| 8 | Rate limiting enabled on all endpoints | Redis-backed bucket per user/IP |
| 9 | Refresh tokens sent as `HttpOnly Secure` cookies | `SameSite=Strict` in prod profile |
| 10 | S3 bucket has no public ACLs | Serve media via signed URLs or CDN |
| 11 | Email verification enforced before publish | Check `users.email_verified` in `ArticleService` |
| 12 | Actuator `/health` is public; all others require ADMIN | `management.endpoints.web.exposure.include=health` |
| 13 | Swagger UI disabled in prod | `springdoc.swagger-ui.enabled=false` |
| 14 | Hikari pool size tuned to DB plan | `maximum-pool-size` ≤ Postgres `max_connections` |
| 15 | Logging configured for structured JSON | `logback-spring.xml` with JSON encoder |
| 16 | All integration tests pass | `./mvnw verify` must be green before every deploy |
| 17 | `.env` is in `.gitignore` | Never commit secrets |
| 18 | `user_profiles` row always created with `users` row | Registration is `@Transactional` covering both inserts |

---

*VibeWrite Implementation Guide — v2.0.0*
