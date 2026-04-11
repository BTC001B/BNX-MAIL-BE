# BTC Tech Mail Server - API Documentation

This document provides technical details for all REST API endpoints available in the **BTC Tech Mail Server** backend.

---

## 1. Global Specifications

- **Base URL**: `http://YOUR_SERVER_IP:8080`
- **Content-Type**: `application/json`
- **Authentication**: Bearer JWT Token in `Authorization` header.
- **Response Format**: All responses are wrapped in a standard `ApiResponse` object.

### Standard Response Wrapper
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": 1712836200000
}
```

---

## 2. Authentication (`/api/auth`)

### 2.1 User Registration
Registers a new user account.
- **URL**: `/api/auth/register`
- **Method**: `POST`
- **Body**:
```json
{
  "username": "siva_kumar",
  "password": "SecurePassword123",
  "firstName": "Siva",
  "lastName": "Kumar",
  "accountType": "PUBLIC" // Options: PUBLIC, BUSINESS, CHILD
}
```
**Note**: The `email` field is optional at this stage and will be created in Step 2.
- **Response**: Returns a `userId` and a `tempToken`. The `tempToken` is required for the next step (Email Creation).

### 2.2 User Login
Authenticates and establishes a session.
- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Body**:
```json
{
  "email": "siva@btctech.shop",
  "password": "SecurePassword123"
}
```
- **Response**: Returns a persistent `token` and user profile details.

---

## 3. Email Management (`/api/emails`)

### 3.1 Create Custom Email
Allocates an actual mailbox on the server.
- **URL**: `/api/emails/create`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <tempToken_or_loginToken>`
- **Body**:
```json
{
  "emailName": "siva",
  "password": "SecurePassword123" 
}
```
- **Response**: Returns `emailId`, `email` address, and `maildirPath`.

### 3.2 List Email Accounts
- **URL**: `/api/emails/list`
- **Method**: `GET`
- **Response Data**:
```json
{
  "count": 1,
  "emails": [
    {
      "id": 1,
      "email": "siva@btctech.shop",
      "isPrimary": true,
      "active": true
    }
  ]
}
```

---

## 4. Mail Operations (`/api/mail`)

### 4.1 Send Email
- **URL**: `/api/mail/send`
- **Method**: `POST`
- **Body**:
```json
{
  "to": "recipient@example.com",
  "cc": "manager@example.com",
  "subject": "Project Update",
  "body": "Hello, here is the update...",
  "isHtml": false
}
```

### 4.2 Get Inbox
- **URL**: `/api/mail/inbox`
- **Method**: `GET`
- **Params**: `limit` (default 50)
- **Response Data**:
```json
{
  "email": "siva@btctech.shop",
  "totalCount": 10,
  "unreadCount": 2,
  "emails": [
    {
      "uid": "123",
      "from": "sender@domain.com",
      "subject": "Greetings",
      "sentDate": "2024-04-11...",
      "isRead": false
    }
  ]
}
```

### 4.3 Get Specific Email
- **URL**: `/api/mail/email/{uid}`
- **Method**: `GET`
- **Response**: Returns full `EmailDTO` including body and attachment list.

---

## 5. Drafts & Collaboration (`/api/mail/drafts`)

### 5.1 Save/Autosave Draft
- **URL**: `/api/mail/drafts`
- **Method**: `POST`
- **Body**:
```json
{
  "id": null, // Use ID for existing drafts
  "to": "client@domain.com",
  "subject": "Draft Proposal",
  "body": "Writing the proposal now...",
  "isHtml": true
}
```

### 5.2 Add Collaborator
- **URL**: `/api/mail/drafts/{id}/collaborators`
- **Method**: `POST`
- **Body**:
```json
{
  "userId": 45,
  "permission": "EDIT" // Options: VIEW, EDIT, SEND
}
```

---

## 6. Business Operations (`/api/business`)

### 6.1 Initialize Domain Verification
- **URL**: `/api/business/domain/init`
- **Method**: `POST`
- **Body**: `{ "organizationId": 1 }`
- **Response Data**:
```json
{
  "verificationToken": "btctech-verify-87623hjb",
  "dnsInstructions": "Add a TXT record for @ with value btctech-verify-..."
}
```

### 6.2 Invite Team Member
- **URL**: `/api/business/invite-member`
- **Method**: `POST`
- **Body**:
```json
{
  "emailName": "jane.doe",
  "firstName": "Jane",
  "lastName": "Doe",
  "role": "ORG_USER"
}
```

---

## 7. Media & Previews (`/api/media`)

### 7.1 Stream Attachment Preview
- **URL**: `/api/media/preview/{draftId}/{thumbFileName}`
- **Method**: `GET`
- **Response**: Returns the raw image stream (`image/jpeg`).
