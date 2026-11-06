# Thesis backend: project overview and conventions


```
com.mateirobescu.thesis
├── auth/                  shared auth pieces: AuthController, AuthService, AuthUtils,
│   │                      SecurityConfig, OpenApiConfig, PasswordEncoderConfiguration
│   ├── token/             TokenPair, TokenUtils
│   ├── jwt/               JwtService, JwtAuthenticationFilter (access tokens)
│   ├── refreshtoken/      opaque, hashed, rotating refresh tokens
│   ├── emailverification/ emailed one-time verification tokens
│   ├── extensionauthcode/ one-time PKCE code for browser-extension sign-in
│   ├── signin/            EmailSignIn*, SignInResult, SignInResponse
│   └── signup/            SignUp*
├── users/  workspaces/  projects/  files/  operations/
├── exception/             ApiException subclasses, GlobalExceptionHandler, ErrorResponse
├── email/  logging/  validation/
```

## Naming conventions

### Auth vocabulary

| Concept | Name | Notes |
|---|---|---|
| Short-lived JWT | **access token** | `JwtService.generateAccessToken` |
| Opaque, rotating, stored hashed | **refresh token** | `RefreshToken` |
| Access + refresh together | **`TokenPair`** | the `tokens` field of `SignInResponse` |
| Emailed verification secret | **email verification token** | `EmailVerificationToken` |
| One-time code the extension redeems | **extension auth code** | `ExtensionAuthCode`. It is a code, never called an "access token". |
| Plain-text password | **`rawPassword`** | on requests, commands and services |
| Stored password hash | **`passwordHash`** | on `User` and `UserCreateCommand` |
| Hashed token or code | **`tokenHash`** / **`codeHash`** | |
| Unhashed token or code | **`rawToken`** / **`rawCode`** | |

"Sign in" and "sign up" are the only terms for authentication. Never use "login" or "register".

### Types

Every use case is `X` followed by a suffix:

- `XRequest`: the HTTP body, with validation annotations.
- `XCommand`: input to a service, with a static `fromRequest(request)`. Commands hold plain ids and values, never entities.
- `XResponse`: the HTTP body returned, with a static `fromX(...)`.
- `XResult`: only when a service must return several things (for example `SignInResult` holds the `TokenPair` and the `User`). Do not add a Result that only wraps a single value.
- `XPatchRequest` → `XPatchCommand` → `XPatch`. The service resolves ids in the command into entities and hands an `XPatch` to the entity's `applyPatch`. Services never take a `*Request`.

Sign-in flows are named after how the user proves who they are: `EmailSignIn*` and `ExtensionSignIn*`. Their shared output is `SignInResult` / `SignInResponse`.

### Service method verbs

| Verb | Meaning | Examples |
|---|---|---|
| `issueX` | create and persist a secret, return the raw value | `issueRefreshToken`, `issueEmailVerificationToken`, `issueAuthCode` |
| `getValidX` | find one that is unexpired and unused, or throw | `getValidRefreshToken`, `getValidEmailVerificationToken` |
| `consumeX` | spend a one-time token or code atomically, return what it unlocks | `consumeEmailVerificationToken`, `consumeAuthCode` |
| `revokeX` / `rotateX` | invalidate a refresh token, or revoke and reissue | `rotateRefreshToken` |
| `signUp`, `signInWithEmail`, `signInWithExtensionAuthCode`, `refresh`, `verifyEmail` | public `AuthService` flows | |

Repository atomic updates are `markUsedAt(id, now)` with a default `markUsed(id)` wrapper.

### Controllers

Method names match the service method they call (`signUp`, `signInWithEmail`, `refresh`, `issueExtensionAuthCode`). A name is never reused for two endpoints.

### Endpoints (kebab-case)

`/auth/sign-up`, `/auth/email-sign-in`, `/auth/extension-sign-in`, `/auth/extension-auth-code`, `/auth/refresh`, `/auth/verify-email`, `/auth/verify-email/info`. Other resources are plural nouns (`/workspaces`, `/projects`, `/projects/{projectId}/files`, `/operations`).

### Configuration keys

Namespaced by owner, with the unit in the key name:

```
auth.jwt.secret
auth.jwt.access-token-validity-minutes
auth.refresh-token.validity-days
auth.email-verification-token.validity-hours
auth.extension-auth-code.validity-seconds
app.frontend.url
app.frontend.verify-email-path
```

### Database

Tables are snake_case plurals (`refresh_tokens`, `extension_auth_codes`). Ids are UUIDv7. Timestamps are `Instant` and called `createdAt`, `expiresAt`, `usedAt`, `revokedAt`, `deletedAt`.

## Layering rules

- Controller: validates the request, maps it to a command, calls one service method, maps the result to a response.
- Service: business rules and transactions. It takes commands and returns entities or results.
- Entities are not exposed over HTTP. Responses are built with `XResponse.fromX`.
- Cross-feature calls go through the other feature's service, never its repository.

## Known deviations

Not yet aligned with the rules above. Do not copy these patterns:

- The create commands (`FileCreateCommand`, `ProjectCreateCommand`, `WorkspaceCreateCommand`, `ExtensionAuthCodeCommand`, `OperationCreateEnvelope`) call `AuthUtils.getCurrentUserId()` inside `fromRequest`, instead of the controller passing the user id in.
- Operation naming is inconsistent: `OperationCreateEnvelope` and `OperationResponseEnvelope`, and the repository method `findByProject_IdAndSeqGreaterThanOrderBySeqAsc`.
- `User.createdDate` should be `createdAt`.
- The soft-delete helpers use different names (`markDeleted`, `markAsDeleted`, `unmarkDeleted`, `restoreFile`).
- Visibility is uneven: some services, repositories and controllers are public and some are package-private.
- Services use field injection style inconsistently: some fields are `private final`, some are package-visible and non-final.
- Dead code: commented-out endpoints, `EmailVerificationResponse.failed()`, the unused `WorkspaceService.getWorkspacesByUser` overloads, `User.unmarkDeleted`.
- `UserService.getAllUsers` does not filter deleted users, and `getUserById` requires a verified, non-deleted user without saying so in its name.
