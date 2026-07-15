# BNX Mail Mobile API Reference

Source checked against the Spring controllers on **15 July 2026**. Give this file to the mobile team. It is intentionally the source of truth for the endpoints currently exposed by this backend.

## Connection and response rules

- **Base URL:** `http(s)://<server>:8080`
- Send JSON as `Content-Type: application/json`, except endpoints marked **multipart**.
- Send `Authorization: Bearer <accessToken>` for every endpoint marked **Auth**. Store the `accessToken` and `refreshToken` returned by login.
- Values in `{braces}` are path parameters. Query parameters follow `?`.
- Unless the response column says **raw**, JSON responses use this wrapper:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {},
  "timestamp": 1712836200000
}
```

`data: null` is a successful action with no returned object. Validation failures and application errors use the same wrapper where applicable, normally with `success: false`, a message, and an HTTP 4xx/5xx status. Do not treat HTTP 200 alone as success—also check `success`.

### Common response data shapes

| Name | `data` / raw response shape |
|---|---|
| `LoginResponseData` | `{ "userId": 1, "username": "siva", "firstName": "Siva", "lastName": "Kumar", "email": "siva@bnxmail.com", "role": "PUBLIC", "accountType": "PERSONAL", "isPrimary": true, "accessToken": "jwt", "refreshToken": "jwt", "accessTokenExpiresIn": 3600000, "refreshTokenExpiresIn": 604800000, "profilePicture": "file.png", "profilePictureUrl": "/api/users/profile-picture/siva", "mailboxes": [{ "emailId": 1, "email": "siva@bnxmail.com", "isPrimary": true }], "isAutoUpgraded": false, "onboarded": true, "loginAt": "2026-07-15T10:30:00" }` |
| `InboxResponse` | `{ "email": "siva@bnxmail.com", "totalCount": 10, "unreadCount": 2, "emails": [EmailDTO] }` |
| `EmailDTO` | `{ "uid": "123", "messageId": "<id>", "from": "sender@example.com", "avatarUrl": "url", "to": "me@example.com", "subject": "Hello", "body": "plain text", "htmlBody": "<p>...</p>", "sentDate": "ISO/date value", "receivedDate": "ISO/date value", "isRead": false, "isStarred": false, "category": "PRIMARY", "folderName": "INBOX", "hasAttachments": false, "attachments": ["file.pdf"], "size": 1234, "labels": [MailLabel] }` |
| `MailLabel` | `{ "id": 1, "userEmail": "me@example.com", "name": "Work", "colorHex": "#FF5733", "parentId": null }` |
| `SignatureDTO` | `{ "id": 1, "name": "Work", "content": "Regards", "isDefault": true, "createdAt": "2026-07-15T10:00:00", "updatedAt": "2026-07-15T10:00:00" }` |
| `AttachmentInfo` | `{ "fileName": "quote.pdf", "filePath": "server-path", "thumbnailPath": "server-path-or-null", "size": 12345 }` |
| `MailDraft` | `{ "id": 1, "mailAccountId": 1, "to": "to@example.com", "cc": "", "bcc": "", "subject": "Draft", "body": "Text", "isHtml": false, "lastOpenedAt": "2026-07-15T10:00:00", "attachmentsJson": "[...]", "status": "DRAFT", "failureReason": null, "createdAt": "...", "updatedAt": "..." }` |
| `SessionResponse` | `{ "id": 1, "ipAddress": "127.0.0.1", "userAgent": "...", "createdAt": "ISO instant", "expiresAt": "ISO instant", "currentSession": true }` |
| `CasboxMessageDto` | `{ "id": 1, "senderEmail": "a@example.com", "receiverEmail": "b@example.com", "subject": "Hi", "body": "Hello", "attachmentsJson": "[]", "status": "SENT", "timestamp": "2026-07-15T10:00:00" }` |
| `ChatDTO` | `{ "id": 1, "name": "Team", "type": "GROUP", "memberEmails": ["a@example.com"], "lastMessage": "Hello", "lastMessageTime": "...", "unreadCount": 0, "creatorEmail": "a@example.com" }` |
| `MessageResponse` | `{ "id": 1, "chatId": 1, "sender": "a@example.com", "content": "Hello", "timestamp": "...", "attachmentsJson": "[]" }` |

## 1. Authentication — `/api/auth`

| Method & path | Auth | Body / query | Response |
|---|---|---|---|
| `POST /register` | No | `RegisterRequest` | wrapper; `{ userId, tempToken }` |
| `GET /username-suggestions?firstName=&lastName=&dob=` | No | Required query values; `dob` is `YYYY-MM-DD` | wrapper; `string[]` |
| `POST /login` | No | `{ "email": "...", "password": "..." }` | wrapper; `LoginResponseData` **or** a 2FA-required object containing `tempToken` |
| `POST /login/2fa` | No | `{ "tempToken": "...", "code": "123456" }` | wrapper; `LoginResponseData` |
| `POST /login/2fa/send-otp` | No | `{ "tempToken": "..." }` | wrapper; `{ "message": "..." }` |
| `POST /login/2fa/verify-otp` | No | `{ "tempToken": "...", "otp": "123456" }` | wrapper; `LoginResponseData` |
| `POST /refresh` | No | `{ "refreshToken": "..." }` | wrapper; `LoginResponseData` |
| `POST /logout` | Auth | `{ "refreshToken": "..." }` | wrapper; `null` |
| `POST /change-password` | Auth | `{ "oldPassword": "...", "newPassword": "..." }` | wrapper; `null` |
| `GET /sessions` | Auth | — | wrapper; `SessionResponse[]` |
| `DELETE /sessions/{sessionId}` | Auth | — | wrapper; `null` |
| `GET /sessions/external` | Auth | — | wrapper; `[{ id, appName, clientId, loggedInAt, ipAddress, userAgent }]` |
| `POST /child/send-parent-otp` | No | `{ "parentEmail": "..." }` | wrapper; `null` |
| `POST /child/verify-parent-otp` | No | `{ "parentEmail": "...", "otp": "..." }` | wrapper; `null` |
| `GET /forgot-password/options?identifier=` | No | Required query `identifier` | wrapper; `{ "recoveryEmail": "...", "phoneNumber": "..." }` |
| `POST /forgot-password/send-otp` | No | `{ "identifier": "...", "method": "EMAIL" }` (`EMAIL` or `PHONE`) | wrapper; `null` |
| `POST /forgot-password/verify-otp` | No | `{ "identifier": "...", "otp": "..." }` | wrapper; `null` |
| `POST /reset-password` | No | `{ "identifier": "...", "otp": "...", "newPassword": "..." }` | wrapper; `null` |

`RegisterRequest`:

```json
{
  "username": "siva_kumar", "password": "SecurePassword123", "firstName": "Siva", "lastName": "Kumar",
  "mode": "PERSONAL", "dob": "2000-08-15", "parentEmail": "parent@example.com",
  "businessName": "BTC Tech", "businessType": "Software", "registrationNumber": "GST123",
  "ownerFirstName": "Siva", "ownerLastName": "Kumar", "domain": "bnxmail.com"
}
```

Use the personal fields for `mode: PERSONAL`; business fields are used for `mode: BUSINESS`. Required-field validation is enforced by the backend.

## 2. User, settings, accounts, onboarding

| Method & path | Auth | Body / query | Response |
|---|---|---|---|
| `GET /api/users/me` | Auth | — | wrapper; current-user object (`id`, name, username, email, role, profile fields) |
| `PATCH /api/users/{id}/approve` | Auth | — | wrapper; approved-user object |
| `GET /api/users/settings` | Auth | — | wrapper; `UserSettingsDTO` |
| `PATCH /api/users/settings` | Auth | `UserSettingsDTO` (send fields to change) | wrapper; updated `UserSettingsDTO` |
| `GET /api/users/activity-logs` | Auth | — | wrapper; activity-log object array |
| `GET /api/users/recovery` | Auth | — | wrapper; `{ "recoveryEmail": "...", "phoneNumber": "..." }` |
| `PATCH /api/users/recovery` | Auth | `{ "recoveryEmail": "...", "phoneNumber": "..." }` | wrapper; `null` |
| `POST /api/emails/create` | Auth | `{ "emailName": "siva", "password": "optional" }` | wrapper; `{ emailId, email, maildirPath, ... }` |
| `GET /api/emails/list` | Auth | — | wrapper; `{ count, emails: [{ id, email, isPrimary, active, ... }] }` |
| `POST /api/emails/{emailId}/set-primary` | Auth | — | wrapper; mailbox result object |
| `GET /api/emails/all?domain=bnxmail.com` | No | Optional `domain` | wrapper; public account directory |
| `POST /api/business/domain/init` | No | `{ "organizationId": 1 }` | wrapper; domain/DNS setup result |
| `POST /api/business/domain/verify` | No | `{ "organizationId": 1 }` | wrapper; verification result |
| `POST /api/business/invite-member` | Auth | `{ "emailName": "new.member", "firstName": "New", "lastName": "Member", "role": "ORG_USER" }` | wrapper; invite result |
| `POST /api/business/accept-invite` | No | `{ "inviteToken": "...", "username": "...", "password": "..." }` | wrapper; accepted-member result |
| `POST /api/business/onboard` | Auth | `{ "businessType": "...", "industry": "...", "companySize": "...", "businessWebsite": "...", "businessAddress": "...", "timeZone": "Asia/Kolkata", "language": "en", "profilePhoto": "...", "companyLogo": "...", "acceptTerms": true }` | wrapper; onboarding result |

`UserSettingsDTO` fields: `phoneNumber`, `location`, `jobTitle`, `inboxNotifications`, `sentNotifications`, `starredNotifications`, `snoozedNotifications`, `soundEnabled`, `vibrationEnabled`, `quietHoursEnabled`, `quietHoursStart`, `quietHoursEnd`, `themeMode`, `accentColor`, `fontSize`, `density`, `profilePictureUrl`, `storageLimit`, `storageUsed`, `twoFactorEnabled`, `biometricsEnabled`, `language`, `undoSendDelay`, `readingPaneMode`.

## 3. Mail send and scheduled mail — `/api/mail`

| Method & path | Auth | Body / query | Response |
|---|---|---|---|
| `POST /send` | Auth | `SendMailRequest` | wrapper; send-result map (message/recipient metadata) |
| `POST /public/send` | `X-Public-Mail-Token` header | `SendMailRequest` | wrapper; send-result map |
| `POST /bulk-send` | Auth | `BulkMailRequest` | wrapper; bulk-send result map |
| `POST /schedule?sendAt=2026-07-16T09:00:00` | Auth | `SendMailRequest`; required ISO local date/time query `sendAt` | wrapper; scheduled-mail result map |
| `GET /scheduled` | Auth | — | wrapper; `{ count, emails: [ScheduledEmail] }` |
| `DELETE /scheduled/{id}` | Auth | — | wrapper; cancellation result map |
| `POST /draft` | Auth | `SendMailRequest` | wrapper; `null` |

`SendMailRequest`:

```json
{
  "to": "recipient@example.com", "cc": "copy@example.com", "bcc": "hidden@example.com",
  "subject": "Project update", "body": "Hello", "fromName": "Siva", "isHtml": false,
  "attachments": [{ "fileName": "quote.pdf", "filePath": "/server/path", "thumbnailPath": null, "size": 12345 }]
}
```

`BulkMailRequest` is `{ "recipients": ["a@example.com", "b@example.com"], "subject": "...", "body": "...", "isHtml": true, "attachments": [AttachmentInfo] }`.

## 4. Mailbox read, folders and message actions — `/api/mail`

All routes below require Auth. A folder query is normally `INBOX`; use the folder that contains the message for message actions/download.

| Method & path | Query | Response |
|---|---|---|
| `GET /inbox` | `limit=50` | wrapper; `InboxResponse` |
| `GET /category/{category}` | `limit=50` | wrapper; `InboxResponse` |
| `GET /sent` | `limit=50` | wrapper; `InboxResponse` |
| `GET /labels/{labelId}` | `limit=50` | wrapper; `InboxResponse` |
| `GET /starred` | `limit=50` | wrapper; `InboxResponse` |
| `GET /trash` | `limit=50` | wrapper; `InboxResponse` |
| `GET /spam` | `limit=50` | wrapper; `InboxResponse` |
| `GET /snoozed` | `limit=50` | wrapper; `InboxResponse` |
| `GET /archive` | `limit=50` | wrapper; `InboxResponse` |
| `GET /draft` | `limit=50` | wrapper; `InboxResponse` |
| `GET /email/{uid}` | — | wrapper; `EmailDTO` |
| `GET /{uid}/attachments/{fileName}` | `folder=INBOX` | **raw binary file**; use response `Content-Type`/`Content-Disposition` |
| `POST /star/{uid}` | `folder=INBOX` | wrapper; `null` (toggles star) |
| `POST /trash/{uid}` | `folder=INBOX` | wrapper; `null` |
| `POST /spam/{uid}` | `folder=INBOX` | wrapper; `null` |
| `POST /snooze/{uid}` | required `wakeUpAt`, `folder=INBOX` | wrapper; `null` |
| `POST /archive/{uid}` | `folder=INBOX` | wrapper; `null` |
| `POST /unarchive/{uid}` | — | wrapper; `null` |
| `POST /restore/{uid}` | — | wrapper; `null` |
| `POST /restore-spam/{uid}` | — | wrapper; `null` |
| `DELETE /permanent/{uid}` | — | wrapper; `null` |
| `POST /read/{uid}` | — | wrapper; `null` |
| `POST /unread/{uid}` | — | wrapper; `null` |
| `POST /unsubscribe` | required `senderEmail` | wrapper; `null` |
| `POST /subscribe` | required `senderEmail` | wrapper; `null` |
| `GET /subscriptions` | — | wrapper; `string[]` |

## 5. Drafts, labels, templates and signatures

All routes in this section require Auth.

| Method & path | Body / query | Response |
|---|---|---|
| `POST /api/mail/drafts` | `DraftRequest` | wrapper; `MailDraft` |
| `GET /api/mail/drafts` | — | wrapper; `MailDraft[]` |
| `GET /api/mail/drafts/{id}` | — | wrapper; `MailDraft` |
| `DELETE /api/mail/drafts/{id}` | — | wrapper; `null` |
| `POST /api/mail/drafts/{id}/attachments` | **multipart** field `file` | wrapper; `AttachmentInfo` |
| `DELETE /api/mail/drafts/{id}/attachments/{fileName}` | — | wrapper; `null` |
| `POST /api/mail/drafts/{id}/send` | — | wrapper; `null` |
| `POST /api/mail/drafts/{id}/collaborators` | `{ "userId": 2, "permission": "VIEWER" }` | wrapper; `null` |
| `GET /api/mail/drafts/{id}/collaborators` | — | wrapper; collaborator array |
| `DELETE /api/mail/drafts/{id}/collaborators/{userId}` | — | wrapper; `null` |
| `GET /api/mail/labels` | — | wrapper; `MailLabel[]` |
| `POST /api/mail/labels` | `{ "name": "Work", "colorHex": "#FF5733", "parentId": null }` | wrapper; `MailLabel` |
| `PUT /api/mail/labels/{id}` | same label body | wrapper; `MailLabel` |
| `DELETE /api/mail/labels/{id}` | — | wrapper; `null` |
| `POST /api/mail/labels/apply/{uid}` | required query `folder`, `labelId` | wrapper; `null` |
| `DELETE /api/mail/labels/remove/{uid}` | required query `folder`, `labelId` | wrapper; `null` |
| `GET /api/signatures` | — | wrapper; `SignatureDTO[]` |
| `POST /api/signatures` | `{ "name": "Work", "content": "Regards", "isDefault": false }` | wrapper; `SignatureDTO` |
| `PUT /api/signatures/{id}` | signature body | wrapper; `SignatureDTO` |
| `DELETE /api/signatures/{id}` | — | wrapper; `null` |
| `PATCH /api/signatures/{id}/default` | — | wrapper; `SignatureDTO` |
| `GET /api/mail/templates` | — | wrapper; `MailTemplate[]` |
| `POST /api/mail/templates` | `{ "name": "Follow up", "subject": "...", "body": "..." }` | wrapper; `MailTemplate` |
| `PUT /api/mail/templates/{id}` | mail-template body | wrapper; `MailTemplate` |
| `DELETE /api/mail/templates/{id}` | — | wrapper; `null` |
| `GET /api/templates?userEmail=` | optional `userEmail` | wrapper; `EmailTemplate[]` |
| `POST /api/templates?userEmail=` | `{ "title": "Welcome", "subject": "...", "body": "...", "category": "...", "isDefault": false }` | wrapper; `EmailTemplate` |
| `PUT /api/templates/{id}?userEmail=` | email-template body | wrapper; `EmailTemplate` |
| `DELETE /api/templates/{id}?userEmail=` | optional `userEmail` | wrapper; `null` |

`DraftRequest` body is `{ "id": null, "mailAccountId": 0, "userId": 0, "to": "...", "cc": "...", "bcc": "...", "subject": "...", "body": "...", "isHtml": false }`. `mailAccountId` and `userId` are overwritten from the authenticated session, so mobile clients may omit them. Collaborator permission is the server enum, normally `VIEWER` or `EDITOR`.

## 6. Profile pictures, vault and media

| Method & path | Auth | Request | Response |
|---|---|---|---|
| `POST /api/users/profile-picture` | Auth | **multipart** field `file` | wrapper; profile-picture result map |
| `DELETE /api/users/profile-picture` | Auth | — | wrapper; `null` |
| `POST /api/emails/{emailId}/profile-picture` | Auth | **multipart** field `file` | wrapper; profile-picture result map |
| `DELETE /api/emails/{emailId}/profile-picture` | Auth | — | wrapper; `null` |
| `GET /api/users/profile-picture/{usernameOrEmail}` | No | — | **raw image binary** (or error JSON) |
| `POST /api/vault/upload` | Auth | **multipart** field `file` | raw vault-file object |
| `GET /api/vault` | Auth | — | raw array of vault metadata `{ id, fileName, contentType, fileSize, uploadedAt, ... }` |
| `GET /api/vault/{id}/download` | Auth | — | **raw binary file** |
| `DELETE /api/vault/{id}` | Auth | — | raw success/error object |
| `GET /api/media/preview/{draftId}/{thumbFileName}` | No | — | **raw thumbnail image binary** |

## 7. Two-factor authentication — `/api/users/2fa`

All routes require Auth. These endpoints return **raw JSON**, not the standard wrapper.

| Method & path | Body | Response |
|---|---|---|
| `POST /setup` | — | `{ "secret": "...", "qrCode": "data-or-url", ... }` |
| `POST /verify` | `{ "code": "123456" }` | `{ "success": true, "message": "..." }` |
| `GET /accounts` | — | authenticator-account array |
| `POST /accounts` | `{ "name": "Phone", "secret": "BASE32SECRET" }` | raw created-account / success object |
| `POST /disable` | — | `{ "success": true, "message": "..." }` |
| `POST /enable` | — | `{ "success": true, "message": "..." }` |

## 8. Casbox — `/api/casbox`

All routes require Auth and return **raw** responses.

| Method & path | Body | Response |
|---|---|---|
| `POST /send` | `{ "receiverEmail": "b@example.com", "subject": "Hi", "body": "Hello", "attachmentsJson": "[]" }` | `CasboxMessageDto` |
| `GET /thread/{contactEmail}` | — | `CasboxMessageDto[]` |
| `GET /` | — | `CasboxMessageDto[]` |
| `PATCH /status` | `{ "messageIds": [1, 2], "status": "READ" }` | HTTP 200, empty body |
| `POST /delivered` | — | HTTP 200, empty body |

## 9. Chat — `/api/chat`

Chat routes return **raw JSON**. Routes that take the current principal (group creation, members, invitations, broadcasts, leave/delete/rename) require Auth; the remaining routes are currently exposed by the controller without an explicit principal.

| Method & path | Body | Response |
|---|---|---|
| `POST /direct` | `{ "user1": "a@example.com", "user2": "b@example.com" }` | `ChatDTO` |
| `POST /group` | `{ "name": "Team", "members": ["a@example.com", "b@example.com"] }` | `ChatDTO` |
| `GET /user/{email}` | — | `ChatDTO[]` |
| `GET /{chatId}/messages` | — | `MessageResponse[]` |
| `POST /message` | `{ "chatId": 1, "sender": "a@example.com", "message": "Hello", "attachmentsJson": "[]" }` | `MessageResponse` |
| `POST /{chatId}/members` | `{ "emails": ["new@example.com"] }` | `ChatDTO` |
| `GET /invitations` | — | `[{ id, chatId, chatName, chatType, inviterEmail, status, createdAt }]` |
| `POST /invitations/{id}/accept` | — | HTTP 200, empty body |
| `POST /invitations/{id}/reject` | — | HTTP 200, empty body |
| `GET /{chatId}/members` | — | `string[]` email addresses |
| `POST /{chatId}/broadcast` | `{ "subject": "Notice", "body": "...", "attachmentsJson": "[]" }` | `{ id, chatId, senderEmail, subject, body, sentDate, attachmentsJson }` |
| `GET /{chatId}/broadcasts` | — | broadcast-response array |
| `POST /{id}/leave` | — | `{ "message": "Left chat successfully" }` |
| `DELETE /{id}` | — | `{ "message": "Chat deleted successfully" }` |
| `PATCH /{id}/name` | `{ "name": "New group name" }` | raw renamed-chat / success object |

## 10. OAuth and identity verification

| Method & path | Auth | Body / query | Response |
|---|---|---|---|
| `POST /api/oauth/authorize` | Auth | `{ "clientId": "...", "redirectUri": "https://...", "state": "optional" }` | wrapper; authorization-code map |
| `POST /api/oauth/token` | No | `{ "grantType": "authorization_code", "code": "...", "clientId": "...", "clientSecret": "..." }` | wrapper; token map |
| `POST /api/verification/initiate/{emailId}` | Auth | — | wrapper; verification-initiation map |
| `GET /api/verification/status/{referenceId}` | No | — | wrapper; `{ referenceId, status, ... }` |
| `POST /api/verification/webhook` | No (provider callback) | provider payload JSON | raw string acknowledgement |

## Mobile implementation notes

1. Refresh the access token through `/api/auth/refresh` when an authenticated call returns 401, then retry the original request once.
2. URL-encode `fileName`, `usernameOrEmail`, and `contactEmail` when putting them in a path.
3. Use `multipart/form-data` only for file uploads; all other bodies are JSON. Do not manually set the multipart boundary in the mobile client.
4. Dates returned by Java may be ISO strings or numeric timestamps depending on the endpoint; parse both safely where possible.
5. The API currently has both legacy `EmailTemplate` (`/api/templates`) and `MailTemplate` (`/api/mail/templates`) resources. Keep their routes and payload field names separate.

