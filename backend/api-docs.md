# VibeWrite REST API Documentation

> **Version:** 2.0.0 · **Base URL:** `/api/v1` · **Format:** JSON · **Schema:** users/user\_profiles split

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Authentication Endpoints](#2-authentication-endpoints)
3. [User Management Endpoints](#3-user-management-endpoints)
4. [Article Endpoints](#4-article-endpoints)
5. [Comment Endpoints](#5-comment-endpoints)
6. [Feed & Discovery Endpoints](#6-feed--discovery-endpoints)
7. [Engagement Endpoints](#7-engagement-endpoints)
8. [Profile & Dashboard Endpoints](#8-profile--dashboard-endpoints)
9. [Admin Endpoints](#9-admin-endpoints)
10. [File Upload Endpoints](#10-file-upload-endpoints)

---

## 1. Introduction

### Project Overview

**VibeWrite** is a modern long-form blogging and content platform — a clean, developer-first alternative to Medium and Substack. It supports the full content lifecycle from draft creation to scheduled publishing, with rich engagement features including threaded comments, claps, bookmarks, and a personalized discovery feed.

This document describes all REST API endpoints exposed by the VibeWrite Spring Boot backend. Each endpoint includes complete request/response examples, error codes, and relevant business rules.

### Base URL

```
https://api.vibewrite.io/api/v1
```

All responses are `application/json` unless otherwise noted.

### Authentication

VibeWrite uses **JWT Bearer Token** authentication.

- Include the token in the `Authorization` header on every protected request:
  ```
  Authorization: Bearer <access_token>
  ```
- Access tokens expire after **15 minutes**. Use the `/auth/refresh` endpoint to obtain a new one.
- Refresh tokens expire after **7 days** and are stored as `HttpOnly` cookies.
- OAuth2 flows (Google, GitHub) ultimately produce the same JWT pair.

### Database Design Note — users / user\_profiles split

The schema separates identity from profile data across two tables:

| Table | Contains | Used by |
|---|---|---|
| `users` | `id`, `email`, `password_hash`, `role`, `status`, `email_verified` | Spring Security, JWT, all FK references |
| `user_profiles` | `username`, `full_name`, `avatar_url`, `bio`, social links, denormalized counts | All display / public-facing responses |

Spring Security loads only the lean `users` row on every authenticated request. Profile data is lazy-loaded via a `@OneToOne(fetch = LAZY)` relationship and only queried when a response requires display fields. All foreign keys across the schema (`articles.author_id`, `comments.author_id`, `bookmarks.user_id`, etc.) reference `users.id`.

### Roles

| Role    | Description                                  |
|---------|----------------------------------------------|
| `USER`  | Default role for all registered users        |
| `ADMIN` | Platform moderators with elevated privileges |

### Common Response Format

**Success wrapper:**
```json
{
  "success": true,
  "data": { },
  "message": "Operation completed successfully",
  "timestamp": "2025-04-11T09:30:00Z"
}
```

**Paginated success wrapper:**
```json
{
  "success": true,
  "data": {
    "content": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 143,
    "totalPages": 8,
    "last": false
  },
  "timestamp": "2025-04-11T09:30:00Z"
}
```

**Error wrapper:**
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Article not found with slug: my-article-slug",
    "details": [ ]
  },
  "timestamp": "2025-04-11T09:30:00Z"
}
```

**Validation error (400):**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "details": [
      { "field": "email", "message": "must be a valid email address" },
      { "field": "password", "message": "must be at least 8 characters" }
    ]
  },
  "timestamp": "2025-04-11T09:30:00Z"
}
```

### Common Error Codes

| HTTP Status | Error Code              | Meaning                                       |
|-------------|-------------------------|-----------------------------------------------|
| 400         | `VALIDATION_FAILED`     | One or more request fields are invalid        |
| 401         | `UNAUTHORIZED`          | Missing or invalid JWT                        |
| 401         | `TOKEN_EXPIRED`         | JWT has expired                               |
| 403         | `FORBIDDEN`             | Valid JWT but insufficient role/ownership     |
| 404         | `RESOURCE_NOT_FOUND`    | Entity does not exist                         |
| 409         | `CONFLICT`              | Duplicate resource (email, slug, username...) |
| 422         | `UNPROCESSABLE_ENTITY`  | Business rule violation                       |
| 429         | `RATE_LIMIT_EXCEEDED`   | Too many requests                             |
| 500         | `INTERNAL_SERVER_ERROR` | Unexpected server error                       |

---

## 2. Authentication Endpoints

### 2.1 Register a New User

**Endpoint:** `POST /api/v1/auth/register`

**Description:** Creates a new user account with email and password. Inserts a row into `users` and a corresponding row into `user_profiles` within a single transaction. Sends a verification email upon successful registration.

**Authentication:** Public

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe"
}
```

| Field      | Type   | Required | Constraints                                    | Table stored in    |
|------------|--------|----------|------------------------------------------------|--------------------|
| `username` | String | Yes      | 3–30 chars, alphanumeric + underscores, unique | `user_profiles`    |
| `email`    | String | Yes      | Valid email format, unique                     | `users`            |
| `password` | String | Yes      | Min 8 chars, must contain uppercase + number   | `users` (hashed)   |
| `fullName` | String | Yes      | 2–100 chars                                    | `user_profiles`    |

**Success Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "emailVerified": false,
    "createdAt": "2025-04-11T09:30:00Z"
  },
  "message": "Registration successful. Please check your email to verify your account."
}
```

**Error Responses:**

| Status | Code                | Scenario                              |
|--------|---------------------|---------------------------------------|
| 400    | `VALIDATION_FAILED` | Invalid email or weak password        |
| 409    | `CONFLICT`          | Email already exists in `users`       |
| 409    | `CONFLICT`          | Username already exists in `user_profiles` |

**Notes:**
- Registration creates a row in `users` (credentials + role) AND a row in `user_profiles` (display data) atomically. If either insert fails, both are rolled back.
- Users must verify their email before they can publish articles. `email_verified` is stored on `users`.
- Unverified accounts can still read and draft articles.

---

### 2.2 Login

**Endpoint:** `POST /api/v1/auth/login`

**Description:** Authenticates a user with email and password. Queries `users` for credential validation, then joins `user_profiles` to build the display fields in the response. Returns a JWT access token and sets an HttpOnly refresh token cookie.

**Authentication:** Public

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
      "username": "john_doe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg",
      "role": "USER",
      "emailVerified": true
    }
  },
  "message": "Login successful"
}
```

The refresh token is returned as a `Set-Cookie` header:
```
Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Max-Age=604800; Path=/api/v1/auth/refresh
```

**Response field sources:**

| Field          | Source table    |
|----------------|-----------------|
| `userId`       | `users.id`      |
| `email`        | `users.email`   |
| `role`         | `users.role`    |
| `emailVerified`| `users.email_verified` |
| `username`     | `user_profiles.username` |
| `fullName`     | `user_profiles.full_name` |
| `avatarUrl`    | `user_profiles.avatar_url` |

**Error Responses:**

| Status | Code                | Scenario                           |
|--------|---------------------|------------------------------------|
| 400    | `VALIDATION_FAILED` | Missing email or password          |
| 401    | `UNAUTHORIZED`      | Invalid email/password combination |
| 403    | `FORBIDDEN`         | Account disabled (`users.status = DISABLED`) |

---

### 2.3 Refresh Access Token

**Endpoint:** `POST /api/v1/auth/refresh`

**Description:** Issues a new access token using a valid refresh token cookie. Queries only the `users` table — no profile join needed. The refresh token is rotated on every call.

**Authentication:** Requires valid `refresh_token` HttpOnly cookie

**Request Headers:**
```
Cookie: refresh_token=<refresh_token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Error Responses:**

| Status | Code            | Scenario                         |
|--------|-----------------|----------------------------------|
| 401    | `TOKEN_EXPIRED` | Refresh token has expired        |
| 401    | `UNAUTHORIZED`  | Invalid or missing refresh token |

---

### 2.4 Logout

**Endpoint:** `POST /api/v1/auth/logout`

**Description:** Invalidates the current refresh token (sets `is_revoked = true` in `refresh_tokens`) and clears the cookie.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

### 2.5 OAuth2 — Initiate Google Login

**Endpoint:** `GET /api/v1/auth/oauth2/google`

**Description:** Redirects the browser to Google's OAuth2 authorization page.

**Authentication:** Public

**Notes:**
- This is a browser redirect endpoint, not a JSON API.
- After Google authorization, Google redirects to the callback URI configured on the server.

---

### 2.6 OAuth2 — Initiate GitHub Login

**Endpoint:** `GET /api/v1/auth/oauth2/github`

**Description:** Redirects the browser to GitHub's OAuth2 authorization page.

**Authentication:** Public

---

### 2.7 OAuth2 — Callback Handler

**Endpoint:** `GET /api/v1/auth/oauth2/callback`

**Description:** Handles the OAuth2 provider callback. Looks up `oauth_identities` by `(provider, provider_user_id)`. If no match, creates a new row in `users`, a new row in `user_profiles`, and a new row in `oauth_identities` — all in one transaction. Issues JWT tokens identical to the login flow.

**Authentication:** Public (validated by OAuth2 state parameter)

**Query Parameters:**

| Parameter | Type   | Required | Description                    |
|-----------|--------|----------|--------------------------------|
| `code`    | String | Yes      | Authorization code from provider |
| `state`   | String | Yes      | CSRF state token               |

**Success Behavior:** Redirects to the frontend with the access token as a URL fragment:
```
https://vibewrite.io/auth/success#access_token=eyJ...&expires_in=900
```

**Notes:**
- For new OAuth2 users, a `username` is auto-generated from the provider's display name and stored in `user_profiles`. The user can change it later via `PUT /users/me/username`.
- `password_hash` is `NULL` for OAuth2-only accounts in `users`.

---

### 2.8 Verify Email

**Endpoint:** `GET /api/v1/auth/verify-email`

**Description:** Verifies a user's email address using the token sent in the verification email. Sets `users.email_verified = true` on success.

**Authentication:** Public

**Query Parameters:**

| Parameter | Type   | Required | Description              |
|-----------|--------|----------|--------------------------|
| `token`   | String | Yes      | Email verification token |

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Email verified successfully. You can now publish articles."
}
```

**Error Responses:**

| Status | Code                | Scenario                        |
|--------|---------------------|---------------------------------|
| 400    | `VALIDATION_FAILED` | Token is expired (72h window)   |
| 404    | `RESOURCE_NOT_FOUND`| Token does not exist            |

---

### 2.9 Request Password Reset

**Endpoint:** `POST /api/v1/auth/forgot-password`

**Description:** Looks up `users.email`. If found, creates a row in `password_reset_tokens` and sends a reset email.

**Authentication:** Public

**Request Body:**
```json
{
  "email": "john@example.com"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "If an account with that email exists, a password reset link has been sent."
}
```

**Notes:**
- Always returns 200 regardless of whether the email exists, to prevent email enumeration.
- Reset tokens expire after **1 hour**.

---

### 2.10 Reset Password

**Endpoint:** `POST /api/v1/auth/reset-password`

**Description:** Validates the token against `password_reset_tokens`, updates `users.password_hash`, and marks the token as used.

**Authentication:** Public

**Request Body:**
```json
{
  "token": "a1b2c3d4-reset-token-here",
  "newPassword": "NewSecurePass456!",
  "confirmPassword": "NewSecurePass456!"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Password reset successful. You can now log in with your new password."
}
```

**Error Responses:**

| Status | Code                | Scenario                              |
|--------|---------------------|---------------------------------------|
| 400    | `VALIDATION_FAILED` | Passwords don't match / weak password |
| 400    | `VALIDATION_FAILED` | Token expired or already used         |
| 404    | `RESOURCE_NOT_FOUND`| Token not found                       |

---

### 2.11 Change Password (Authenticated)

**Endpoint:** `PUT /api/v1/auth/change-password`

**Description:** Verifies `currentPassword` against `users.password_hash`, then updates it. Revokes all existing `refresh_tokens` for the user.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "currentPassword": "SecurePass123!",
  "newPassword": "NewSecurePass456!",
  "confirmPassword": "NewSecurePass456!"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Password changed successfully. All other sessions have been invalidated."
}
```

**Notes:**
- Changing the password sets `is_revoked = true` on all rows in `refresh_tokens` for this user, forcing re-login on other devices.
- Not available for OAuth2-only accounts (where `password_hash` is NULL). Returns 422 in that case.

---

## 3. User Management Endpoints

### 3.1 Get Current User (Auth + Profile)

**Endpoint:** `GET /api/v1/users/me`

**Description:** Returns the full merged view of the authenticated user. Assembles the response by joining `users` (auth fields) with `user_profiles` (display fields). This is the only endpoint that returns both `email` and profile data together.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "bio": "Writer. Thinker. Coffee enthusiast.",
    "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg",
    "website": "https://johndoe.com",
    "twitterHandle": "@johndoe",
    "linkedInUrl": "https://linkedin.com/in/johndoe",
    "role": "USER",
    "emailVerified": true,
    "followersCount": 142,
    "followingCount": 38,
    "articlesCount": 17,
    "createdAt": "2025-01-15T08:00:00Z",
    "updatedAt": "2025-04-10T14:22:00Z"
  }
}
```

**Response field sources:**

| Field             | Source table                          |
|-------------------|---------------------------------------|
| `userId`          | `users.id`                            |
| `email`           | `users.email`                         |
| `role`            | `users.role`                          |
| `emailVerified`   | `users.email_verified`                |
| `createdAt`       | `users.created_at`                    |
| `updatedAt`       | `user_profiles.updated_at`            |
| `username`        | `user_profiles.username`              |
| `fullName`        | `user_profiles.full_name`             |
| `bio`             | `user_profiles.bio`                   |
| `avatarUrl`       | `user_profiles.avatar_url`            |
| `website`         | `user_profiles.website`               |
| `twitterHandle`   | `user_profiles.twitter_handle`        |
| `linkedInUrl`     | `user_profiles.linkedin_url`          |
| `followersCount`  | `user_profiles.followers_count`       |
| `followingCount`  | `user_profiles.following_count`       |
| `articlesCount`   | `user_profiles.articles_count`        |

---

### 3.2 Get My Profile Only

**Endpoint:** `GET /api/v1/users/me/profile`

**Description:** Returns only the `user_profiles` row for the authenticated user. Useful for lightweight clients (mobile, widgets) that have already loaded auth data and only need to refresh display fields without re-fetching credentials.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "username": "john_doe",
    "fullName": "John Doe",
    "bio": "Writer. Thinker. Coffee enthusiast.",
    "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg",
    "website": "https://johndoe.com",
    "twitterHandle": "@johndoe",
    "linkedInUrl": "https://linkedin.com/in/johndoe",
    "followersCount": 142,
    "followingCount": 38,
    "articlesCount": 17,
    "lastActiveAt": "2025-04-11T08:00:00Z",
    "updatedAt": "2025-04-10T14:22:00Z"
  }
}
```

**Notes:** This endpoint queries only `user_profiles`. No join to `users` is performed — it does not return `email`, `role`, or `emailVerified`.

---

### 3.3 Get Public User Profile

**Endpoint:** `GET /api/v1/users/{username}`

**Description:** Returns the public profile of any user by username. Performs a lookup against `user_profiles.username` (which carries a unique index). Private fields (`email`, `role`, `status`) are excluded.

**Authentication:** Public

**Path Variables:**

| Variable   | Type   | Description         |
|------------|--------|---------------------|
| `username` | String | The user's username |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "username": "john_doe",
    "fullName": "John Doe",
    "bio": "Writer. Thinker. Coffee enthusiast.",
    "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg",
    "website": "https://johndoe.com",
    "twitterHandle": "@johndoe",
    "followersCount": 142,
    "followingCount": 38,
    "articlesCount": 17,
    "isFollowing": false,
    "createdAt": "2025-01-15T08:00:00Z"
  }
}
```

**Notes:**
- Lookup is performed against `user_profiles.username`. Returns 404 if no profile exists with that username.
- `isFollowing` is `true` only if the requester is authenticated and has a row in `user_follows` for this user; `false` for unauthenticated requests.
- All fields in this response come from `user_profiles`. `createdAt` reflects `users.created_at` via join.

**Error Responses:**

| Status | Code                | Scenario                          |
|--------|---------------------|-----------------------------------|
| 404    | `RESOURCE_NOT_FOUND`| Username does not exist in `user_profiles` |

---

### 3.4 Update Current User Profile

**Endpoint:** `PUT /api/v1/users/me`

**Description:** Updates display fields in `user_profiles` for the authenticated user. All fields are optional; only provided fields are updated (partial update). Username cannot be changed via this endpoint — use `PUT /users/me/username` instead.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "fullName": "John A. Doe",
  "bio": "Writer. Thinker. Coffee enthusiast. Now with better bio.",
  "website": "https://johndoe.dev",
  "twitterHandle": "@johndoe_writes",
  "linkedInUrl": "https://linkedin.com/in/johndoe-writer"
}
```

| Field           | Type   | Required | Constraints        | Table updated   |
|-----------------|--------|----------|--------------------|-----------------|
| `fullName`      | String | No       | 2–100 chars        | `user_profiles` |
| `bio`           | String | No       | Max 300 chars      | `user_profiles` |
| `website`       | String | No       | Valid URL          | `user_profiles` |
| `twitterHandle` | String | No       | Max 50 chars       | `user_profiles` |
| `linkedInUrl`   | String | No       | Valid LinkedIn URL | `user_profiles` |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "username": "john_doe",
    "fullName": "John A. Doe",
    "bio": "Writer. Thinker. Coffee enthusiast. Now with better bio.",
    "updatedAt": "2025-04-11T10:00:00Z"
  },
  "message": "Profile updated successfully"
}
```

**Notes:**
- All fields in this request update the `user_profiles` row only. The `users` row is not touched.
- `username` is not accepted in this request body. To change username, use `PUT /api/v1/users/me/username`.

---

### 3.5 Change Username

**Endpoint:** `PUT /api/v1/users/me/username`

**Description:** Changes the authenticated user's username. Updates `user_profiles.username`. Enforces a 30-day cooldown between username changes per user.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "newUsername": "john_doe_v2"
}
```

| Field         | Type   | Required | Constraints                                 |
|---------------|--------|----------|---------------------------------------------|
| `newUsername` | String | Yes      | 3–30 chars, alphanumeric + underscores, unique |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "oldUsername": "john_doe",
    "newUsername": "john_doe_v2",
    "updatedAt": "2025-04-11T10:00:00Z"
  },
  "message": "Username updated successfully"
}
```

**Error Responses:**

| Status | Code                  | Scenario                                         |
|--------|-----------------------|--------------------------------------------------|
| 409    | `CONFLICT`            | Username already taken in `user_profiles`        |
| 422    | `UNPROCESSABLE_ENTITY`| 30-day cooldown has not elapsed since last change |

**Notes:**
- Username uniqueness is enforced by the `UNIQUE` constraint on `user_profiles.username`.
- The previous username is immediately available for other users to claim.
- All existing public URLs that used the old username (`/users/old_username`) will return 404 after the change.

---

### 3.6 Upload Avatar

**Endpoint:** `POST /api/v1/users/me/avatar`

**Description:** Uploads a new avatar image. Stores the CDN URL in `user_profiles.avatar_url`. Replaces any existing avatar.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

**Request Body (multipart):**

| Field    | Type | Required | Constraints                            |
|----------|------|----------|----------------------------------------|
| `avatar` | File | Yes      | JPEG/PNG/WebP, max 5 MB, min 100×100px |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe_v2.jpg"
  },
  "message": "Avatar updated successfully"
}
```

**Error Responses:**

| Status | Code                | Scenario                             |
|--------|---------------------|--------------------------------------|
| 400    | `VALIDATION_FAILED` | File too large or unsupported format |
| 413    | `PAYLOAD_TOO_LARGE` | Request body exceeds 10 MB limit     |

---

### 3.7 Delete Avatar

**Endpoint:** `DELETE /api/v1/users/me/avatar`

**Description:** Sets `user_profiles.avatar_url` to `NULL`, reverting to the auto-generated default avatar.

**Authentication:** JWT Required (USER)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Avatar removed. Default avatar restored."
}
```

---

### 3.8 List a User's Published Articles

**Endpoint:** `GET /api/v1/users/{username}/articles`

**Description:** Looks up `user_profiles` by `username`, resolves `user_id`, then queries `articles` where `author_id = user_id` and `status = PUBLISHED`.

**Authentication:** Public

**Path Variables:**

| Variable   | Type   | Description         |
|------------|--------|---------------------|
| `username` | String | The user's username |

**Query Parameters:**

| Parameter | Type    | Required | Default            | Description              |
|-----------|---------|----------|--------------------|--------------------------|
| `page`    | Integer | No       | 0                  | Zero-based page index    |
| `size`    | Integer | No       | 10                 | Items per page (max 50)  |
| `sort`    | String  | No       | `publishedAt,desc` | Sort field and direction |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
        "title": "The Art of Slow Mornings",
        "slug": "the-art-of-slow-mornings",
        "excerpt": "In a world obsessed with productivity, there's radical power in doing nothing slowly.",
        "featuredImageUrl": "https://cdn.vibewrite.io/articles/slow-mornings.jpg",
        "readingTime": 5,
        "viewCount": 1204,
        "clapCount": 87,
        "commentCount": 23,
        "tags": ["lifestyle", "mindfulness"],
        "publishedAt": "2025-03-20T08:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 17,
    "totalPages": 2,
    "last": false
  }
}
```

---

### 3.9 Get User's Followers

**Endpoint:** `GET /api/v1/users/{username}/followers`

**Description:** Resolves `username` via `user_profiles`, then queries `user_follows` where `following_id = user_id`. Returns profile display data for each follower by joining `user_profiles`.

**Authentication:** Public

**Path Variables:**

| Variable   | Type   | Description         |
|------------|--------|---------------------|
| `username` | String | The user's username |

**Query Parameters:**

| Parameter | Type    | Required | Default | Description             |
|-----------|---------|----------|---------|-------------------------|
| `page`    | Integer | No       | 0       | Zero-based page index   |
| `size`    | Integer | No       | 20      | Items per page (max 50) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": "usr_02HXK8Z9T3FGQB2M4NR7VPYWCD",
        "username": "jane_writes",
        "fullName": "Jane Smith",
        "avatarUrl": "https://cdn.vibewrite.io/avatars/jane_writes.jpg",
        "bio": "Product designer & writer.",
        "isFollowing": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 142,
    "totalPages": 8,
    "last": false
  }
}
```

**Notes:** All display fields (`username`, `fullName`, `avatarUrl`, `bio`) come from `user_profiles`. `isFollowing` is computed against `user_follows` for the authenticated requester.

---

### 3.10 Get User's Following

**Endpoint:** `GET /api/v1/users/{username}/following`

**Description:** Resolves `username` via `user_profiles`, then queries `user_follows` where `follower_id = user_id`. Returns profile display data for each followed user.

**Authentication:** Public

**Path Variables / Query Parameters:** Same as [3.9 Get User's Followers](#39-get-users-followers).

---

## 4. Article Endpoints

### 4.1 Create Article (Draft)

**Endpoint:** `POST /api/v1/articles`

**Description:** Creates a new article in `DRAFT` status. Sets `articles.author_id` to the authenticated user's `users.id`.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "The Art of Slow Mornings",
  "content": "<p>In a world obsessed with productivity...</p>",
  "excerpt": "In a world obsessed with productivity, there's radical power in doing nothing slowly.",
  "featuredImageUrl": "https://cdn.vibewrite.io/articles/slow-mornings.jpg",
  "tags": ["lifestyle", "mindfulness"],
  "categoryId": "cat_wellness",
  "metaTitle": "The Art of Slow Mornings | VibeWrite",
  "metaDescription": "Discover why slowing down in the morning can transform your entire day.",
  "canonicalUrl": null
}
```

| Field              | Type     | Required | Constraints                              |
|--------------------|----------|----------|------------------------------------------|
| `title`            | String   | Yes      | 5–200 chars                              |
| `content`          | String   | Yes      | HTML string, max 500 KB                  |
| `excerpt`          | String   | No       | Max 300 chars; auto-generated if absent  |
| `featuredImageUrl` | String   | No       | Valid CDN URL from `/api/v1/uploads`     |
| `tags`             | String[] | No       | Max 5 tags, each 2–30 chars              |
| `categoryId`       | String   | No       | Must reference an existing `categories` row |
| `metaTitle`        | String   | No       | Max 70 chars; defaults to `title`        |
| `metaDescription`  | String   | No       | Max 160 chars                            |
| `canonicalUrl`     | String   | No       | Valid URL for cross-posted content       |

**Success Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "title": "The Art of Slow Mornings",
    "slug": "the-art-of-slow-mornings",
    "status": "DRAFT",
    "readingTime": 5,
    "author": {
      "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
      "username": "john_doe",
      "fullName": "John Doe",
      "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg"
    },
    "tags": ["lifestyle", "mindfulness"],
    "createdAt": "2025-04-11T10:00:00Z",
    "updatedAt": "2025-04-11T10:00:00Z"
  },
  "message": "Article created as draft"
}
```

**Notes:**
- Slugs are auto-generated from the title and guaranteed unique (e.g., `the-art-of-slow-mornings-2` if taken).
- Reading time is calculated server-side from content word count (~200 words/min).
- The `author` sub-object in the response is populated from `user_profiles` via join on `users.id`.

---

### 4.2 Get Article by Slug (Public)

**Endpoint:** `GET /api/v1/articles/{slug}`

**Description:** Returns the full content of a published article. Increments view count for authenticated users and unique anonymous visitors (tracked by IP hash + 24h window in `article_views`).

**Authentication:** Public (returns additional fields for authenticated users)

**Path Variables:**

| Variable | Type   | Description      |
|----------|--------|------------------|
| `slug`   | String | The article slug |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "title": "The Art of Slow Mornings",
    "slug": "the-art-of-slow-mornings",
    "content": "<p>In a world obsessed with productivity...</p>",
    "excerpt": "In a world obsessed with productivity, there's radical power in doing nothing slowly.",
    "featuredImageUrl": "https://cdn.vibewrite.io/articles/slow-mornings.jpg",
    "readingTime": 5,
    "status": "PUBLISHED",
    "viewCount": 1205,
    "clapCount": 87,
    "commentCount": 23,
    "bookmarkCount": 34,
    "tags": ["lifestyle", "mindfulness"],
    "category": {
      "categoryId": "cat_wellness",
      "name": "Wellness",
      "slug": "wellness"
    },
    "author": {
      "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
      "username": "john_doe",
      "fullName": "John Doe",
      "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg",
      "bio": "Writer. Thinker. Coffee enthusiast.",
      "followersCount": 142,
      "isFollowing": false
    },
    "seo": {
      "metaTitle": "The Art of Slow Mornings | VibeWrite",
      "metaDescription": "Discover why slowing down in the morning can transform your entire day.",
      "canonicalUrl": null
    },
    "userInteraction": {
      "hasClapped": false,
      "hasBookmarked": true,
      "clapsGiven": 0
    },
    "publishedAt": "2025-03-20T08:00:00Z",
    "updatedAt": "2025-03-21T09:15:00Z"
  }
}
```

**Notes:**
- `author` display fields (`username`, `fullName`, `avatarUrl`, `bio`, `followersCount`) come from `user_profiles` joined via `articles.author_id → users.id → user_profiles.user_id`.
- `userInteraction` fields are `null` for unauthenticated requests.
- DRAFT and SCHEDULED articles return `404` for non-owners.

**Error Responses:**

| Status | Code                | Scenario                              |
|--------|---------------------|---------------------------------------|
| 404    | `RESOURCE_NOT_FOUND`| Slug does not exist or not published  |

---

### 4.3 Get Article by ID (Owner/Admin)

**Endpoint:** `GET /api/v1/articles/id/{articleId}`

**Description:** Returns full article data including draft content. Only accessible by the article owner or an admin.

**Authentication:** JWT Required (USER — owner, or ADMIN)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `articleId` | String | The article ID |

**Success Response (200 OK):** Same schema as [4.2](#42-get-article-by-slug-public) plus:
```json
{
  "data": {
    "...": "all fields from 4.2",
    "scheduledAt": null,
    "analytics": {
      "viewCount": 1205,
      "uniqueReaders": 890,
      "avgReadTimeSeconds": 210,
      "clapCount": 87
    }
  }
}
```

---

### 4.4 Update Article

**Endpoint:** `PUT /api/v1/articles/{articleId}`

**Description:** Updates an existing article. Works on articles in any status.

**Authentication:** JWT Required (USER — owner only)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `articleId` | String | The article ID |

**Request Body:** Same schema as [4.1 Create Article](#41-create-article-draft). All fields optional.

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "title": "The Art of Slow Mornings (Revised)",
    "slug": "the-art-of-slow-mornings",
    "status": "PUBLISHED",
    "updatedAt": "2025-04-11T11:30:00Z"
  },
  "message": "Article updated successfully"
}
```

**Notes:**
- Updating the title of a DRAFT generates a new slug. Updating the title of a PUBLISHED article does **not** change the slug (to preserve existing URLs).

---

### 4.5 Delete Article

**Endpoint:** `DELETE /api/v1/articles/{articleId}`

**Description:** Permanently deletes an article and all associated comments, claps, bookmarks, and views via CASCADE.

**Authentication:** JWT Required (USER — owner, or ADMIN)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `articleId` | String | The article ID |

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Article deleted successfully"
}
```

**Error Responses:**

| Status | Code                | Scenario                  |
|--------|---------------------|---------------------------|
| 403    | `FORBIDDEN`         | Not the owner or admin    |
| 404    | `RESOURCE_NOT_FOUND`| Article does not exist    |

---

### 4.6 Publish Article

**Endpoint:** `POST /api/v1/articles/{articleId}/publish`

**Description:** Transitions an article from `DRAFT` to `PUBLISHED`. Checks `users.email_verified` before allowing publication. Sends a publication confirmation email and notifies all followers.

**Authentication:** JWT Required (USER — owner only)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `articleId` | String | The article ID |

**Request Body:** _(empty)_

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "slug": "the-art-of-slow-mornings",
    "status": "PUBLISHED",
    "publishedAt": "2025-04-11T12:00:00Z"
  },
  "message": "Article published successfully"
}
```

**Error Responses:**

| Status | Code                  | Scenario                                          |
|--------|-----------------------|---------------------------------------------------|
| 403    | `FORBIDDEN`           | Not the owner                                     |
| 422    | `UNPROCESSABLE_ENTITY`| `users.email_verified = false`                    |
| 422    | `UNPROCESSABLE_ENTITY`| Article already published                         |
| 422    | `UNPROCESSABLE_ENTITY`| Article content is empty                          |

---

### 4.7 Unpublish Article

**Endpoint:** `POST /api/v1/articles/{articleId}/unpublish`

**Description:** Returns a PUBLISHED article back to DRAFT. Sets `articles.status = DRAFT` and clears `articles.published_at`.

**Authentication:** JWT Required (USER — owner only)

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "status": "DRAFT"
  },
  "message": "Article unpublished and returned to drafts"
}
```

---

### 4.8 Schedule Article

**Endpoint:** `POST /api/v1/articles/{articleId}/schedule`

**Description:** Sets `articles.status = SCHEDULED` and `articles.scheduled_at` to the given UTC timestamp. A background job publishes it automatically at that time.

**Authentication:** JWT Required (USER — owner only)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `articleId` | String | The article ID |

**Request Body:**
```json
{
  "scheduledAt": "2025-04-15T09:00:00Z"
}
```

| Field         | Type     | Required | Constraints                              |
|---------------|----------|----------|------------------------------------------|
| `scheduledAt` | ISO 8601 | Yes      | Must be at least 5 minutes in the future |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "status": "SCHEDULED",
    "scheduledAt": "2025-04-15T09:00:00Z"
  },
  "message": "Article scheduled for publication"
}
```

**Error Responses:**

| Status | Code                  | Scenario                       |
|--------|-----------------------|--------------------------------|
| 422    | `UNPROCESSABLE_ENTITY`| `scheduledAt` is in the past   |
| 422    | `UNPROCESSABLE_ENTITY`| Article is already published   |

---

### 4.9 Cancel Scheduled Publication

**Endpoint:** `DELETE /api/v1/articles/{articleId}/schedule`

**Description:** Cancels a scheduled publication. Sets `articles.status = DRAFT` and clears `articles.scheduled_at`.

**Authentication:** JWT Required (USER — owner only)

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "status": "DRAFT"
  },
  "message": "Scheduled publication cancelled"
}
```

---

### 4.10 List Categories

**Endpoint:** `GET /api/v1/categories`

**Description:** Returns all rows from the `categories` table.

**Authentication:** Public

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "categoryId": "cat_wellness",
      "name": "Wellness",
      "slug": "wellness",
      "articleCount": 523
    },
    {
      "categoryId": "cat_tech",
      "name": "Technology",
      "slug": "technology",
      "articleCount": 1204
    }
  ]
}
```

---

### 4.11 Get Articles by Tag

**Endpoint:** `GET /api/v1/tags/{tag}/articles`

**Description:** Returns published articles associated with a given tag via `article_tags` join.

**Authentication:** Public

**Path Variables:**

| Variable | Type   | Description                |
|----------|--------|----------------------------|
| `tag`    | String | The tag name (URL-encoded) |

**Query Parameters:**

| Parameter | Type    | Required | Default            | Description              |
|-----------|---------|----------|--------------------|--------------------------|
| `page`    | Integer | No       | 0                  | Zero-based page index    |
| `size`    | Integer | No       | 20                 | Items per page (max 50)  |
| `sort`    | String  | No       | `publishedAt,desc` | Sort field and direction |

**Success Response (200 OK):** Paginated list of article summaries (same schema as article summary objects in 3.8).

---

### 4.12 Get Article Stats

**Endpoint:** `GET /api/v1/articles/{articleId}/stats`

**Description:** Returns engagement statistics for a specific article. Aggregates from `article_views`, `article_claps`, `comments`, and `bookmarks`.

**Authentication:** JWT Required (USER — owner only)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `articleId` | String | The article ID |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "totalViews": 1205,
    "uniqueReaders": 890,
    "avgReadTimeSeconds": 210,
    "readThroughRate": 0.74,
    "clapCount": 87,
    "commentCount": 23,
    "bookmarkCount": 34,
    "referrers": [
      { "source": "twitter.com", "visits": 430 },
      { "source": "direct", "visits": 320 }
    ],
    "viewsOverTime": [
      { "date": "2025-04-10", "views": 142 },
      { "date": "2025-04-11", "views": 78 }
    ]
  }
}
```

---

## 5. Comment Endpoints

### 5.1 List Comments for an Article

**Endpoint:** `GET /api/v1/articles/{slug}/comments`

**Description:** Returns root-level comments for an article (`parent_comment_id IS NULL`). Replies are included up to one level deep. Author display fields come from `user_profiles` via join.

**Authentication:** Public

**Path Variables:**

| Variable | Type   | Description      |
|----------|--------|------------------|
| `slug`   | String | The article slug |

**Query Parameters:**

| Parameter        | Type    | Required | Default          | Description                                     |
|------------------|---------|----------|------------------|-------------------------------------------------|
| `page`           | Integer | No       | 0                | Zero-based page index                           |
| `size`           | Integer | No       | 20               | Root comments per page (max 50)                 |
| `sort`           | String  | No       | `createdAt,desc` | Sort: `createdAt,asc`, `likeCount,desc`         |
| `includeReplies` | Boolean | No       | `true`           | Whether to include nested replies               |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "commentId": "cmt_01HXKD3T3FGQB2M4NR7VPYWCD",
        "content": "This resonated so deeply. Thank you for writing this.",
        "author": {
          "userId": "usr_02HXK8Z9T3FGQB2M4NR7VPYWCD",
          "username": "jane_writes",
          "fullName": "Jane Smith",
          "avatarUrl": "https://cdn.vibewrite.io/avatars/jane_writes.jpg"
        },
        "likeCount": 14,
        "hasLiked": false,
        "isEdited": false,
        "status": "APPROVED",
        "repliesCount": 3,
        "replies": [
          {
            "commentId": "cmt_02HXKD3T3FGQB2M4NR7VPYWCD",
            "content": "Glad it did, Jane.",
            "author": {
              "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
              "username": "john_doe",
              "fullName": "John Doe",
              "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg"
            },
            "likeCount": 5,
            "hasLiked": false,
            "isEdited": false,
            "parentCommentId": "cmt_01HXKD3T3FGQB2M4NR7VPYWCD",
            "createdAt": "2025-03-20T09:05:00Z"
          }
        ],
        "createdAt": "2025-03-20T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 23,
    "totalPages": 2,
    "last": false
  }
}
```

---

### 5.2 Add a Comment

**Endpoint:** `POST /api/v1/articles/{slug}/comments`

**Description:** Inserts a row into `comments` with `author_id = users.id` and `parent_comment_id = NULL`. Triggers a `NEW_COMMENT` notification to the article author.

**Authentication:** JWT Required (USER)

**Path Variables:**

| Variable | Type   | Description      |
|----------|--------|------------------|
| `slug`   | String | The article slug |

**Request Body:**
```json
{
  "content": "This resonated so deeply. Thank you for writing this."
}
```

| Field     | Type   | Required | Constraints  |
|-----------|--------|----------|--------------|
| `content` | String | Yes      | 1–2000 chars |

**Success Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "commentId": "cmt_01HXKD3T3FGQB2M4NR7VPYWCD",
    "content": "This resonated so deeply. Thank you for writing this.",
    "author": {
      "userId": "usr_02HXK8Z9T3FGQB2M4NR7VPYWCD",
      "username": "jane_writes",
      "fullName": "Jane Smith",
      "avatarUrl": "https://cdn.vibewrite.io/avatars/jane_writes.jpg"
    },
    "likeCount": 0,
    "hasLiked": false,
    "isEdited": false,
    "status": "APPROVED",
    "repliesCount": 0,
    "replies": [],
    "createdAt": "2025-04-11T12:00:00Z"
  },
  "message": "Comment posted"
}
```

**Error Responses:**

| Status | Code                  | Scenario                             |
|--------|-----------------------|--------------------------------------|
| 422    | `UNPROCESSABLE_ENTITY`| Article is not published             |

---

### 5.3 Reply to a Comment

**Endpoint:** `POST /api/v1/comments/{commentId}/replies`

**Description:** Inserts a row into `comments` with `parent_comment_id` set to the given comment's ID. Replies are capped at one level deep — if the target comment itself has a parent, the reply is attached to the root comment instead. Triggers a `NEW_REPLY` notification to the parent comment's author.

**Authentication:** JWT Required (USER)

**Path Variables:**

| Variable    | Type   | Description             |
|-------------|--------|-------------------------|
| `commentId` | String | The parent comment's ID |

**Request Body:**
```json
{
  "content": "Glad it did, Jane. Thank you for reading."
}
```

**Success Response (201 Created):** Same schema as a comment object with `parentCommentId` populated.

**Error Responses:**

| Status | Code                  | Scenario                              |
|--------|-----------------------|---------------------------------------|
| 404    | `RESOURCE_NOT_FOUND`  | Parent comment does not exist         |
| 422    | `UNPROCESSABLE_ENTITY`| Cannot reply to a deleted comment     |

---

### 5.4 Update a Comment

**Endpoint:** `PUT /api/v1/comments/{commentId}`

**Description:** Updates `comments.content` and sets `comments.is_edited = true`. Only available within 15 minutes of the original `created_at`.

**Authentication:** JWT Required (USER — comment author only)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `commentId` | String | The comment ID |

**Request Body:**
```json
{
  "content": "This resonated so deeply — especially the part about rituals."
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "commentId": "cmt_01HXKD3T3FGQB2M4NR7VPYWCD",
    "content": "This resonated so deeply — especially the part about rituals.",
    "isEdited": true,
    "updatedAt": "2025-04-11T12:10:00Z"
  }
}
```

**Error Responses:**

| Status | Code                  | Scenario                          |
|--------|-----------------------|-----------------------------------|
| 403    | `FORBIDDEN`           | Not the comment author            |
| 422    | `UNPROCESSABLE_ENTITY`| Edit window expired (>15 minutes) |

---

### 5.5 Delete a Comment

**Endpoint:** `DELETE /api/v1/comments/{commentId}`

**Description:** Soft-deletes a comment by setting `comments.content = '[comment removed]'` and `comments.status = REMOVED`. Available to the comment author (`comments.author_id`), the article owner, or an admin.

**Authentication:** JWT Required (USER — comment author, article owner, or ADMIN)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `commentId` | String | The comment ID |

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Comment deleted"
}
```

---

### 5.6 Like / Unlike a Comment

**Endpoint:** `POST /api/v1/comments/{commentId}/like`

**Description:** Toggles a like on a comment. Inserts or deletes a row in `comment_likes(user_id, comment_id)`. Updates `comments.like_count` accordingly.

**Authentication:** JWT Required (USER)

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "commentId": "cmt_01HXKD3T3FGQB2M4NR7VPYWCD",
    "likeCount": 15,
    "hasLiked": true
  }
}
```

---

### 5.7 Get Replies for a Comment

**Endpoint:** `GET /api/v1/comments/{commentId}/replies`

**Description:** Returns all replies (`parent_comment_id = commentId`) with pagination. Used to lazy-load replies when `includeReplies=false` in the main comment list.

**Authentication:** Public

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `commentId` | String | The comment ID |

**Query Parameters:**

| Parameter | Type    | Required | Default | Description             |
|-----------|---------|----------|---------|-------------------------|
| `page`    | Integer | No       | 0       | Zero-based page index   |
| `size`    | Integer | No       | 20      | Items per page (max 50) |

---

## 6. Feed & Discovery Endpoints

### 6.1 Get Home Feed

**Endpoint:** `GET /api/v1/feed`

**Description:** Returns a personalized feed of articles. For authenticated users, blends articles from followed authors (`user_follows`) and followed tags (`tag_follows`) with platform recommendations. For anonymous users, returns a curated trending feed.

**Authentication:** Optional (richer personalization when authenticated)

**Request Headers:**
```
Authorization: Bearer <access_token>   (optional)
```

**Query Parameters:**

| Parameter | Type    | Required | Default   | Description                                                |
|-----------|---------|----------|-----------|------------------------------------------------------------|
| `page`    | Integer | No       | 0         | Zero-based page index                                      |
| `size`    | Integer | No       | 20        | Items per page (max 50)                                    |
| `type`    | String  | No       | `for_you` | Feed type: `for_you`, `following`, `latest`, `recommended` |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
        "title": "The Art of Slow Mornings",
        "slug": "the-art-of-slow-mornings",
        "excerpt": "In a world obsessed with productivity...",
        "featuredImageUrl": "https://cdn.vibewrite.io/articles/slow-mornings.jpg",
        "readingTime": 5,
        "viewCount": 1205,
        "clapCount": 87,
        "commentCount": 23,
        "tags": ["lifestyle", "mindfulness"],
        "author": {
          "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
          "username": "john_doe",
          "fullName": "John Doe",
          "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg",
          "isFollowing": false
        },
        "hasBookmarked": false,
        "hasClapped": false,
        "feedReason": "trending",
        "publishedAt": "2025-03-20T08:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 500,
    "totalPages": 25,
    "last": false
  }
}
```

**Notes:**
- `type=following` requires authentication; returns 401 if not authenticated.
- `feedReason` values: `following_author`, `following_tag`, `trending`, `recommended`, `latest`.
- Author display fields come from `user_profiles` via join.

---

### 6.2 Get Trending Articles

**Endpoint:** `GET /api/v1/articles/trending`

**Description:** Returns the most-engaged articles over a configurable time window, ranked by a weighted score of `view_count`, `clap_count`, `comment_count`, and `bookmark_count` from the `articles` table.

**Authentication:** Public

**Query Parameters:**

| Parameter  | Type    | Required | Default | Description                                       |
|------------|---------|----------|---------|---------------------------------------------------|
| `period`   | String  | No       | `week`  | Time window: `day`, `week`, `month`, `all_time`   |
| `page`     | Integer | No       | 0       | Zero-based page index                             |
| `size`     | Integer | No       | 20      | Items per page (max 50)                           |
| `category` | String  | No       | _(all)_ | Filter by `categories.slug`                       |
| `tag`      | String  | No       | _(all)_ | Filter by `tags.name`                             |

**Success Response (200 OK):** Paginated list of article summaries, same schema as feed items.

---

### 6.3 Search Articles

**Endpoint:** `GET /api/v1/search`

**Description:** Full-text search across article titles, content, and tags using PostgreSQL GIN index. Author name search joins `user_profiles`. Returns relevance-ranked results.

**Authentication:** Public

**Query Parameters:**

| Parameter  | Type    | Required | Default     | Description                                                           |
|------------|---------|----------|-------------|-----------------------------------------------------------------------|
| `q`        | String  | Yes      | —           | Search query (min 2 chars, max 200 chars)                             |
| `type`     | String  | No       | `articles`  | Scope: `articles`, `authors`, `tags`, `all`                           |
| `category` | String  | No       | _(all)_     | Filter by `categories.slug`                                           |
| `tag`      | String  | No       | _(all)_     | Filter by `tags.name`                                                 |
| `authorId` | String  | No       | _(all)_     | Restrict to a specific `users.id`                                     |
| `dateFrom` | String  | No       | _(none)_    | ISO 8601 date filter (published after)                                |
| `dateTo`   | String  | No       | _(none)_    | ISO 8601 date filter (published before)                               |
| `sortBy`   | String  | No       | `relevance` | Sort: `relevance`, `publishedAt`, `viewCount`, `clapCount`            |
| `page`     | Integer | No       | 0           | Zero-based page index                                                 |
| `size`     | Integer | No       | 20          | Items per page (max 50)                                               |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "query": "slow mornings",
    "totalResults": 38,
    "articles": {
      "content": [
        {
          "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
          "title": "The Art of Slow Mornings",
          "slug": "the-art-of-slow-mornings",
          "excerpt": "...there's radical power in doing nothing <em>slowly</em>...",
          "highlightedTitle": "The Art of <em>Slow Mornings</em>",
          "relevanceScore": 0.97,
          "author": {
            "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
            "username": "john_doe",
            "fullName": "John Doe",
            "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg"
          },
          "tags": ["lifestyle", "mindfulness"],
          "readingTime": 5,
          "publishedAt": "2025-03-20T08:00:00Z"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 38
    }
  }
}
```

**Error Responses:**

| Status | Code                | Scenario                              |
|--------|---------------------|---------------------------------------|
| 400    | `VALIDATION_FAILED` | Query is empty or too short           |
| 429    | `RATE_LIMIT_EXCEEDED`| Search rate limit hit (30 req/min)   |

---

### 6.4 Get Recommended Articles

**Endpoint:** `GET /api/v1/articles/recommended`

**Description:** Returns articles algorithmically recommended based on the user's reading history (`article_views`), followed tags (`tag_follows`), and collaborative filtering. Falls back to trending articles for unauthenticated users.

**Authentication:** Optional

**Query Parameters:**

| Parameter | Type    | Required | Default | Description                                   |
|-----------|---------|----------|---------|-----------------------------------------------|
| `page`    | Integer | No       | 0       | Zero-based page index                         |
| `size`    | Integer | No       | 20      | Items per page (max 50)                       |
| `exclude` | String  | No       | —       | Comma-separated article IDs to exclude        |

**Success Response (200 OK):** Paginated list of article summaries with `feedReason: "recommended"`.

---

### 6.5 List Articles by Category

**Endpoint:** `GET /api/v1/categories/{categorySlug}/articles`

**Description:** Returns published articles where `articles.category_id` matches the given category slug.

**Authentication:** Public

**Path Variables:**

| Variable       | Type   | Description       |
|----------------|--------|-------------------|
| `categorySlug` | String | The category slug |

**Query Parameters:** Same pagination + sort parameters as [4.11](#411-get-articles-by-tag).

**Error Responses:**

| Status | Code                | Scenario                      |
|--------|---------------------|-------------------------------|
| 404    | `RESOURCE_NOT_FOUND`| Category slug does not exist  |

---

## 7. Engagement Endpoints

### 7.1 Clap for an Article

**Endpoint:** `POST /api/v1/articles/{slug}/clap`

**Description:** Upserts a row in `article_claps(user_id, article_id)` and increments by `count`. Total claps per user per article are capped at 50 by a `CHECK` constraint on `article_claps.clap_count`. Updates the denormalized `articles.clap_count`.

**Authentication:** JWT Required (USER)

**Path Variables:**

| Variable | Type   | Description      |
|----------|--------|------------------|
| `slug`   | String | The article slug |

**Request Body:**
```json
{
  "count": 5
}
```

| Field   | Type    | Required | Constraints                         |
|---------|---------|----------|-------------------------------------|
| `count` | Integer | Yes      | 1–50; total per user capped at 50   |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "totalClapCount": 92,
    "userClapCount": 5,
    "userTotalClapsOnArticle": 5,
    "remainingClaps": 45
  }
}
```

**Error Responses:**

| Status | Code                  | Scenario                                        |
|--------|-----------------------|-------------------------------------------------|
| 403    | `FORBIDDEN`           | Attempting to clap for own article              |
| 422    | `UNPROCESSABLE_ENTITY`| User has exhausted all 50 claps on this article |

---

### 7.2 Get User Clap Status for an Article

**Endpoint:** `GET /api/v1/articles/{slug}/clap`

**Description:** Returns the row from `article_claps` for the authenticated user and this article.

**Authentication:** JWT Required (USER)

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "hasClapped": true,
    "userClapCount": 5,
    "remainingClaps": 45
  }
}
```

---

### 7.3 Bookmark an Article

**Endpoint:** `POST /api/v1/articles/{slug}/bookmark`

**Description:** Toggles a bookmark. Inserts `bookmarks(user_id, article_id)` if it doesn't exist; deletes it if it does. Updates the denormalized `articles.bookmark_count`.

**Authentication:** JWT Required (USER)

**Path Variables:**

| Variable | Type   | Description      |
|----------|--------|------------------|
| `slug`   | String | The article slug |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "bookmarked": true
  },
  "message": "Article saved to your reading list"
}
```

---

### 7.4 List Bookmarks

**Endpoint:** `GET /api/v1/users/me/bookmarks`

**Description:** Returns all rows from `bookmarks` for the authenticated user, joined with `articles` for article data.

**Authentication:** JWT Required (USER)

**Query Parameters:**

| Parameter | Type    | Required | Default             | Description             |
|-----------|---------|----------|---------------------|-------------------------|
| `page`    | Integer | No       | 0                   | Zero-based page index   |
| `size`    | Integer | No       | 20                  | Items per page (max 50) |
| `sort`    | String  | No       | `bookmarkedAt,desc` | Sort field and direction|

**Success Response (200 OK):** Paginated list of article summaries with an additional `bookmarkedAt` timestamp (`bookmarks.created_at`).

---

### 7.5 Remove Bookmark

**Endpoint:** `DELETE /api/v1/articles/{slug}/bookmark`

**Description:** Deletes the row from `bookmarks(user_id, article_id)`. Decrements `articles.bookmark_count`.

**Authentication:** JWT Required (USER)

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "bookmarked": false
  },
  "message": "Article removed from your reading list"
}
```

---

### 7.6 Follow a User

**Endpoint:** `POST /api/v1/users/{username}/follow`

**Description:** Resolves `username` via `user_profiles` to get `user_id`, then inserts `user_follows(follower_id, following_id)`. Increments `user_profiles.followers_count` for the followed user and `user_profiles.following_count` for the follower. Triggers a `NEW_FOLLOWER` notification.

**Authentication:** JWT Required (USER)

**Path Variables:**

| Variable   | Type   | Description        |
|------------|--------|--------------------|
| `username` | String | Username to follow |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "targetUserId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "username": "john_doe",
    "isFollowing": true,
    "followersCount": 143
  },
  "message": "You are now following john_doe"
}
```

**Error Responses:**

| Status | Code                  | Scenario                          |
|--------|-----------------------|-----------------------------------|
| 404    | `RESOURCE_NOT_FOUND`  | Username does not exist           |
| 422    | `UNPROCESSABLE_ENTITY`| Cannot follow yourself            |

---

### 7.7 Unfollow a User

**Endpoint:** `DELETE /api/v1/users/{username}/follow`

**Description:** Deletes the row from `user_follows`. Decrements the relevant `user_profiles.followers_count` and `following_count`.

**Authentication:** JWT Required (USER)

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "isFollowing": false,
    "followersCount": 142
  },
  "message": "You have unfollowed john_doe"
}
```

---

### 7.8 Follow a Tag

**Endpoint:** `POST /api/v1/tags/{tag}/follow`

**Description:** Inserts `tag_follows(user_id, tag_id)`. Increments `tags.follower_count`.

**Authentication:** JWT Required (USER)

**Path Variables:**

| Variable | Type   | Description        |
|----------|--------|--------------------|
| `tag`    | String | Tag name to follow |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "tag": "mindfulness",
    "isFollowing": true,
    "followersCount": 8421
  }
}
```

---

### 7.9 Unfollow a Tag

**Endpoint:** `DELETE /api/v1/tags/{tag}/follow`

**Description:** Deletes the row from `tag_follows`. Decrements `tags.follower_count`.

**Authentication:** JWT Required (USER)

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "tag": "mindfulness",
    "isFollowing": false
  }
}
```

---

### 7.10 Get Followed Tags

**Endpoint:** `GET /api/v1/users/me/following/tags`

**Description:** Returns all rows from `tag_follows` for the authenticated user, joined with `tags`.

**Authentication:** JWT Required (USER)

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": [
    { "tag": "mindfulness", "followersCount": 8421 },
    { "tag": "productivity", "followersCount": 12330 }
  ]
}
```

---

## 8. Profile & Dashboard Endpoints

### 8.1 Get Author Dashboard Summary

**Endpoint:** `GET /api/v1/dashboard`

**Description:** Returns an overview of the authenticated user's publishing activity. Aggregates across `articles`, `article_views`, `article_claps`, `comments`, and `bookmarks`. `followersCount` is read directly from `user_profiles.followers_count`.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "totalArticles": 17,
    "publishedArticles": 14,
    "draftArticles": 2,
    "scheduledArticles": 1,
    "totalViews": 24380,
    "totalClaps": 1204,
    "totalComments": 312,
    "totalBookmarks": 548,
    "followersCount": 142,
    "last30Days": {
      "views": 3420,
      "claps": 187,
      "newFollowers": 14
    },
    "topArticles": [
      {
        "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
        "title": "The Art of Slow Mornings",
        "slug": "the-art-of-slow-mornings",
        "viewCount": 4820,
        "clapCount": 312
      }
    ]
  }
}
```

---

### 8.2 List My Articles

**Endpoint:** `GET /api/v1/dashboard/articles`

**Description:** Returns all articles where `articles.author_id = users.id` of the authenticated user, including drafts, scheduled, and published. Supports filtering by `articles.status`.

**Authentication:** JWT Required (USER)

**Query Parameters:**

| Parameter | Type    | Required | Default          | Description                                |
|-----------|---------|----------|------------------|--------------------------------------------|
| `status`  | String  | No       | _(all)_          | Filter: `DRAFT`, `PUBLISHED`, `SCHEDULED`  |
| `page`    | Integer | No       | 0                | Zero-based page index                      |
| `size`    | Integer | No       | 20               | Items per page (max 50)                    |
| `sort`    | String  | No       | `updatedAt,desc` | Sort field and direction                   |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
        "title": "The Art of Slow Mornings",
        "slug": "the-art-of-slow-mornings",
        "status": "PUBLISHED",
        "readingTime": 5,
        "viewCount": 1205,
        "clapCount": 87,
        "commentCount": 23,
        "tags": ["lifestyle", "mindfulness"],
        "scheduledAt": null,
        "publishedAt": "2025-03-20T08:00:00Z",
        "updatedAt": "2025-03-21T09:15:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 17,
    "totalPages": 1,
    "last": true
  }
}
```

---

### 8.3 Get Article Analytics

**Endpoint:** `GET /api/v1/dashboard/articles/{articleId}/analytics`

**Description:** Returns detailed time-series analytics for a specific article. Aggregates `article_views.read_time_seconds`, `article_views.read_through`, and view counts grouped by the requested granularity.

**Authentication:** JWT Required (USER — owner only)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `articleId` | String | The article ID |

**Query Parameters:**

| Parameter | Type   | Required | Default | Description                              |
|-----------|--------|----------|---------|------------------------------------------|
| `period`  | String | No       | `30d`   | Time window: `7d`, `30d`, `90d`, `all`  |
| `groupBy` | String | No       | `day`   | Granularity: `day`, `week`, `month`      |

**Success Response (200 OK):** Same schema as [4.12 Get Article Stats](#412-get-article-stats).

---

### 8.4 Get Notifications

**Endpoint:** `GET /api/v1/notifications`

**Description:** Returns rows from `notifications` where `recipient_id = users.id`. Actor display name and avatar come from `user_profiles` via join on `notifications.actor_id`.

**Authentication:** JWT Required (USER)

**Query Parameters:**

| Parameter | Type    | Required | Default | Description                                        |
|-----------|---------|----------|---------|----------------------------------------------------|
| `read`    | Boolean | No       | _(all)_ | Filter: `true` (read only), `false` (unread only)  |
| `page`    | Integer | No       | 0       | Zero-based page index                              |
| `size`    | Integer | No       | 20      | Items per page (max 50)                            |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "unreadCount": 4,
    "content": [
      {
        "notificationId": "notif_01HXKE3T3FGQB2M4NR7VPYWCD",
        "type": "NEW_COMMENT",
        "message": "jane_writes commented on your article: \"The Art of Slow Mornings\"",
        "isRead": false,
        "actor": {
          "userId": "usr_02HXK8Z9T3FGQB2M4NR7VPYWCD",
          "username": "jane_writes",
          "avatarUrl": "https://cdn.vibewrite.io/avatars/jane_writes.jpg"
        },
        "targetType": "ARTICLE",
        "targetId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
        "targetSlug": "the-art-of-slow-mornings",
        "createdAt": "2025-04-11T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 14,
    "totalPages": 1,
    "last": true
  }
}
```

**Notification Types:** `NEW_COMMENT`, `NEW_REPLY`, `NEW_FOLLOWER`, `ARTICLE_PUBLISHED`, `ARTICLE_FEATURED`

---

### 8.5 Mark Notification as Read

**Endpoint:** `PUT /api/v1/notifications/{notificationId}/read`

**Description:** Sets `notifications.is_read = true` for the given notification. Only the recipient can mark their own notifications.

**Authentication:** JWT Required (USER)

**Path Variables:**

| Variable         | Type   | Description         |
|------------------|--------|---------------------|
| `notificationId` | String | The notification ID |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "notificationId": "notif_01HXKE3T3FGQB2M4NR7VPYWCD",
    "isRead": true
  }
}
```

---

### 8.6 Mark All Notifications as Read

**Endpoint:** `PUT /api/v1/notifications/read-all`

**Description:** Sets `is_read = true` on all unread `notifications` rows where `recipient_id = users.id`.

**Authentication:** JWT Required (USER)

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "All notifications marked as read",
  "data": {
    "updatedCount": 4
  }
}
```

---

## 9. Admin Endpoints

All admin endpoints require `ADMIN` role. `role` is stored on `users.role` and checked by Spring Security.

### 9.1 List All Users (Admin)

**Endpoint:** `GET /api/v1/admin/users`

**Description:** Returns a paginated list of all platform users. Response is assembled by joining `users` and `user_profiles`. If a `user_profiles` row is missing (rare edge case during registration failure), profile fields return `null`.

**Authentication:** JWT Required (ADMIN)

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**

| Parameter | Type    | Required | Default          | Description                                       |
|-----------|---------|----------|------------------|---------------------------------------------------|
| `search`  | String  | No       | —                | Search across `users.email`, `user_profiles.username`, `user_profiles.full_name` |
| `status`  | String  | No       | _(all)_          | Filter by `users.status`: `ACTIVE`, `DISABLED`    |
| `role`    | String  | No       | _(all)_          | Filter by `users.role`: `USER`, `ADMIN`           |
| `page`    | Integer | No       | 0                | Zero-based page index                             |
| `size`    | Integer | No       | 20               | Items per page (max 100)                          |
| `sort`    | String  | No       | `createdAt,desc` | Sort field and direction                          |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
        "email": "john@example.com",
        "username": "john_doe",
        "fullName": "John Doe",
        "avatarUrl": "https://cdn.vibewrite.io/avatars/john_doe.jpg",
        "role": "USER",
        "status": "ACTIVE",
        "emailVerified": true,
        "followersCount": 142,
        "articlesCount": 17,
        "lastLoginAt": "2025-04-10T08:00:00Z",
        "createdAt": "2025-01-15T08:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 14820,
    "totalPages": 742,
    "last": false
  }
}
```

---

### 9.2 Get Single User (Admin)

**Endpoint:** `GET /api/v1/admin/users/{userId}`

**Description:** Returns full merged account details for one user by `users.id`. Joins `users` and `user_profiles`.

**Authentication:** JWT Required (ADMIN)

**Path Variables:**

| Variable | Type   | Description                    |
|----------|--------|--------------------------------|
| `userId` | String | The user's `users.id`          |

**Success Response (200 OK):** Full merged user object with all fields from both `users` and `user_profiles`, including `emailVerified`, `role`, `status`, `lastLoginAt`, `createdAt`.

---

### 9.3 Update User Role

**Endpoint:** `PUT /api/v1/admin/users/{userId}/role`

**Description:** Updates `users.role`. This field is on the `users` table — no profile join required.

**Authentication:** JWT Required (ADMIN)

**Request Body:**
```json
{
  "role": "ADMIN"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "role": "ADMIN"
  },
  "message": "User role updated to ADMIN"
}
```

---

### 9.4 Disable / Enable User Account

**Endpoint:** `PUT /api/v1/admin/users/{userId}/status`

**Description:** Updates `users.status`. Disabled users fail Spring Security's `isAccountNonLocked()` check and cannot log in or publish. No profile join required.

**Authentication:** JWT Required (ADMIN)

**Request Body:**
```json
{
  "status": "DISABLED",
  "reason": "Repeated violation of community guidelines"
}
```

| Field    | Type   | Required | Description                       |
|----------|--------|----------|-----------------------------------|
| `status` | String | Yes      | `ACTIVE` or `DISABLED`            |
| `reason` | String | No       | Internal note (not shown to user) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_01HXK8Z9T3FGQB2M4NR7VPYWCD",
    "status": "DISABLED"
  },
  "message": "User account disabled"
}
```

---

### 9.5 List Flagged Comments (Admin)

**Endpoint:** `GET /api/v1/admin/comments`

**Description:** Returns all comments with `status = FLAGGED` or `PENDING_REVIEW`. Author display data joined from `user_profiles`.

**Authentication:** JWT Required (ADMIN)

**Query Parameters:**

| Parameter | Type    | Required | Default   | Description                                    |
|-----------|---------|----------|-----------|------------------------------------------------|
| `status`  | String  | No       | `FLAGGED` | Filter: `FLAGGED`, `PENDING_REVIEW`, `REMOVED` |
| `page`    | Integer | No       | 0         | Zero-based page index                          |
| `size`    | Integer | No       | 20        | Items per page (max 100)                       |

**Success Response (200 OK):** Paginated list of full comment objects with associated article and author information.

---

### 9.6 Moderate a Comment (Admin)

**Endpoint:** `PUT /api/v1/admin/comments/{commentId}/moderate`

**Description:** Updates `comments.status` to `APPROVED`, `FLAGGED`, or `REMOVED`. Optionally sends an email to the comment author (looked up via `user_profiles.user_id → users.email`).

**Authentication:** JWT Required (ADMIN)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `commentId` | String | The comment ID |

**Request Body:**
```json
{
  "action": "REMOVE",
  "reason": "Violates community guidelines — harassment",
  "notifyAuthor": true
}
```

| Field          | Type    | Required | Description                               |
|----------------|---------|----------|-------------------------------------------|
| `action`       | String  | Yes      | `APPROVE`, `FLAG`, `REMOVE`               |
| `reason`       | String  | No       | Moderation reason; required if `REMOVE`   |
| `notifyAuthor` | Boolean | No       | Send email to comment author (default: false) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "commentId": "cmt_01HXKD3T3FGQB2M4NR7VPYWCD",
    "status": "REMOVED",
    "moderatedAt": "2025-04-11T13:00:00Z",
    "moderatedBy": "admin_01HXK"
  }
}
```

---

### 9.7 Feature / Unfeature an Article (Admin)

**Endpoint:** `PUT /api/v1/admin/articles/{articleId}/feature`

**Description:** Sets `articles.is_featured = true/false`. Featured articles are surfaced in the discovery feed and category pages.

**Authentication:** JWT Required (ADMIN)

**Request Body:**
```json
{
  "featured": true
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "articleId": "art_01HXKBZT3FGQB2M4NR7VPYWCD",
    "featured": true
  },
  "message": "Article marked as featured"
}
```

---

### 9.8 Get Platform Analytics (Admin)

**Endpoint:** `GET /api/v1/admin/analytics`

**Description:** Returns aggregate platform-level metrics by querying `users`, `articles`, `article_views`, `article_claps`, `comments`, and `bookmarks`.

**Authentication:** JWT Required (ADMIN)

**Query Parameters:**

| Parameter | Type   | Required | Default | Description                            |
|-----------|--------|----------|---------|----------------------------------------|
| `period`  | String | No       | `30d`   | Time range: `7d`, `30d`, `90d`, `1y`  |

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "period": "30d",
    "users": {
      "total": 14820,
      "newThisPeriod": 1240,
      "activeThisPeriod": 6320
    },
    "articles": {
      "total": 42310,
      "publishedThisPeriod": 3140,
      "totalViews": 1248000
    },
    "engagement": {
      "totalClaps": 184200,
      "totalComments": 22400,
      "totalBookmarks": 56800
    }
  }
}
```

---

### 9.9 Delete Any Article (Admin)

**Endpoint:** `DELETE /api/v1/admin/articles/{articleId}`

**Description:** Permanently deletes any article regardless of ownership. Cascades to `comments`, `article_claps`, `bookmarks`, `article_views`, and `article_tags`.

**Authentication:** JWT Required (ADMIN)

**Path Variables:**

| Variable    | Type   | Description    |
|-------------|--------|----------------|
| `articleId` | String | The article ID |

**Request Body:**
```json
{
  "reason": "Violates platform content policy — misinformation",
  "notifyAuthor": true
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Article deleted by admin"
}
```

---

## 10. File Upload Endpoints

### 10.1 Upload Featured Image

**Endpoint:** `POST /api/v1/uploads/images/featured`

**Description:** Uploads a featured image. Inserts a row into `uploaded_images` with `uploader_id = users.id`. Returns CDN URLs to be stored in `articles.featured_image_url`.

**Authentication:** JWT Required (USER)

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

**Request Body (multipart):**

| Field   | Type | Required | Constraints                                        |
|---------|------|----------|----------------------------------------------------|
| `image` | File | Yes      | JPEG/PNG/WebP, max 10 MB, min 600×315px (16:9 recommended) |

**Success Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "imageId": "img_01HXKF3T3FGQB2M4NR7VPYWCD",
    "originalUrl": "https://cdn.vibewrite.io/articles/originals/img_01HXKF.jpg",
    "thumbnailUrl": "https://cdn.vibewrite.io/articles/thumbnails/img_01HXKF.jpg",
    "webpUrl": "https://cdn.vibewrite.io/articles/webp/img_01HXKF.webp",
    "width": 1400,
    "height": 788,
    "fileSizeBytes": 245800,
    "mimeType": "image/jpeg"
  }
}
```

**Notes:**
- Images are stored on CDN and served with aggressive cache headers.
- Multiple sizes are auto-generated: original, 800×450 thumbnail, and WebP variants.
- `uploaded_images.is_attached` is set to `false` initially. It flips to `true` when an article referencing the URL is saved. Rows where `is_attached = false` and `expires_at` has passed are purged by a background job (48h window).

**Error Responses:**

| Status | Code                | Scenario                             |
|--------|---------------------|--------------------------------------|
| 400    | `VALIDATION_FAILED` | Unsupported format or file too large |
| 413    | `PAYLOAD_TOO_LARGE` | Multipart body exceeds 15 MB         |

---

### 10.2 Upload In-Article Image

**Endpoint:** `POST /api/v1/uploads/images/inline`

**Description:** Uploads an image for inline use within article content. Inserts into `uploaded_images`. Returns a CDN URL the rich text editor embeds as an `<img>` src.

**Authentication:** JWT Required (USER)

**Request Body (multipart):**

| Field   | Type | Required | Constraints                 |
|---------|------|----------|-----------------------------|
| `image` | File | Yes      | JPEG/PNG/WebP/GIF, max 8 MB |

**Success Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "imageId": "img_02HXKF3T3FGQB2M4NR7VPYWCD",
    "url": "https://cdn.vibewrite.io/inline/img_02HXKF.jpg",
    "webpUrl": "https://cdn.vibewrite.io/inline/webp/img_02HXKF.webp",
    "width": 1200,
    "height": 800,
    "fileSizeBytes": 183400
  }
}
```

---

### 10.3 Delete Uploaded Image

**Endpoint:** `DELETE /api/v1/uploads/images/{imageId}`

**Description:** Deletes the row from `uploaded_images` and removes the file from CDN. Only the uploader (`uploaded_images.uploader_id = users.id`) can delete. Cannot delete if `is_attached = true` and article is PUBLISHED.

**Authentication:** JWT Required (USER — uploader only)

**Path Variables:**

| Variable  | Type   | Description  |
|-----------|--------|--------------|
| `imageId` | String | The image ID |

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Image deleted successfully"
}
```

**Error Responses:**

| Status | Code                | Scenario                                  |
|--------|---------------------|-------------------------------------------|
| 403    | `FORBIDDEN`         | Not the uploader                          |
| 409    | `CONFLICT`          | Image is attached to a published article  |
| 404    | `RESOURCE_NOT_FOUND`| Image does not exist                      |

---

*End of VibeWrite API Documentation — v2.0.0*

---

> **Rate Limits:** Most endpoints are limited to **100 requests/minute** per authenticated user. The search endpoint is limited to **30 requests/minute**. Auth endpoints (`/auth/login`, `/auth/register`) are limited to **10 requests/minute** per IP. Rate limit headers are included in all responses:
> ```
> X-RateLimit-Limit: 100
> X-RateLimit-Remaining: 94
> X-RateLimit-Reset: 1744368060
> ```
