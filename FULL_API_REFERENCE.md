# BTC Tech Mail Server - Full API Reference

This document provides the definitive guide to all REST APIs in the BTC Tech Mail Server backend. All responses follow a standard wrapper format.

---

## 1. Global Specifications

- **Base URL**: `http://localhost:8080`
- **Security**: Bearer Token (JWT) is required for most endpoints.
- **Response Wrapper**:
```json
{
  "success": true,
  "message": "String",
  "data": { ... },
  "timestamp": 1712900000000
}
```

---

## 2. Authentication (`/api/auth`)

### 2.1 Register User
- **URL**: `/api/auth/register`
- **Method**: `POST`
- **Body**:
```json
{
  "username": "string",
  "password": "string (min 8 chars)",
  "firstName": "string",
  "lastName": "string",
  "mode": "PERSONAL | BUSINESS",
  "dob": "YYYY-MM-DD",
  "parentEmail": "string (for CHILD users)",
  "businessName": "string",
  "businessType": "string",
  "registrationNumber": "string",
  "ownerFirstName": "string",
  "ownerLastName": "string",
  "domain": "string"
}
```
- **Response**: `ApiResponse<Map<String, Object>>` (returns `userId`)

### 2.2 Login
- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Body**:
```json
{
  "email": "string",
  "password": "string"
}
```
- **Response**: `ApiResponse<LoginResponseData>` (includes `accessToken`, `refreshToken`, and profile)

### 2.3 Token Refresh
- **URL**: `/api/auth/refresh`
- **Method**: `POST`
- **Body**:
```json
{
  "refreshToken": "string"
}
```
- **Response**: `ApiResponse<LoginResponseData>`

### 2.4 Logout
- **URL**: `/api/auth/logout`
- **Method**: `POST`
- **Auth**: Required
- **Body**:
```json
{
  "refreshToken": "string"
}
```

---

## 3. Mail Operations (`/api/mail`)

### 3.1 Send Mail
- **URL**: `/api/mail/send`
- **Method**: `POST`
- **Auth**: Required
- **Body**:
```json
{
  "to": "string",
  "cc": "string",
  "bcc": "string",
  "subject": "string",
  "body": "string",
  "isHtml": boolean,
  "attachments": [ "AttachmentInfo" ]
}
```

### 3.2 Get Inbox
- **URL**: `/api/mail/inbox`
- **Method**: `GET`
- **Auth**: Required
- **Params**: `limit` (default 50)
- **Response**: `ApiResponse<InboxResponse>`

### 3.3 Read Specific Email
- **URL**: `/api/mail/email/{uid}`
- **Method**: `GET`
- **Auth**: Required
- **Response**: `ApiResponse<EmailDTO>`

---

## 4. Drafts & Collaboration (`/api/mail/drafts`)

### 4.1 Save Draft
- **URL**: `/api/mail/drafts`
- **Method**: `POST`
- **Auth**: Required
- **Body**:
```json
{
  "id": Long, (optional for updates)
  "to": "string",
  "subject": "string",
  "body": "string",
  "isHtml": boolean
}
```

### 4.2 Add Collaborator
- **URL**: `/api/mail/drafts/{id}/collaborators`
- **Method**: `POST`
- **Auth**: Required
- **Body**:
```json
{
  "userId": Long,
  "permission": "VIEW | EDIT | SEND"
}
```

---

## 5. Email Management (`/api/emails`)

### 5.1 Create New Mailbox
- **URL**: `/api/emails/create`
- **Method**: `POST`
- **Auth**: Required
- **Body**:
```json
{
  "emailName": "string (lowercase, alphanumeric)",
  "password": "string (optional, defaults to account password)"
}
```

### 5.2 List User Mailboxes
- **URL**: `/api/emails/list`
- **Method**: `GET`
- **Auth**: Required

---

## 6. Business Management (`/api/business`)

### 6.1 Invite Team Member
- **URL**: `/api/business/invite-member`
- **Method**: `POST`
- **Auth**: Required
- **Body**:
```json
{
  "emailName": "string",
  "firstName": "string",
  "lastName": "string",
  "role": "string"
}
```

### 6.2 Accept Team Invite
- **URL**: `/api/business/accept-invite`
- **Method**: `POST`
- **Body**:
```json
{
  "inviteToken": "string",
  "username": "string",
  "password": "string"
}
```

---

*Note: For detailed field constraints and raw JSON models, please refer to the live Swagger UI at `/swagger-ui/index.html`.*
