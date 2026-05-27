# Feature Specification: Authentication & Session Management

This document defines the specific behaviors, rules, and constraints for client-side authentication and session persistence, extending the high-level goals in [app_spec.md](file:///home/hansenji/src/abs-client-app/specs/app_spec.md).

---

## 1. Feature Context & Constraints

The application must authenticate with a self-hosted Audiobookshelf server using a username and password. Because users enter arbitrary connection URLs, the client is responsible for normalizing the input host string before initiating requests.

---

## 2. Specific Behavioral Rules

### A. Host URL Normalization
1. **Whitespace Trimming**: Remove any leading or trailing whitespace characters from the user-entered URL.
2. **Trailing Slash Removal**: Strip any trailing slash (`/`) from the end of the URL to ensure a clean base host string (e.g., `my.server.com/` becomes `my.server.com`).
3. **Protocol Prepending**: If the input URL does not start with either `http://` or `https://`, default to prepending `https://` (e.g., `my.server.com` becomes `https://my.server.com`).
4. **Endpoint Resolution**: The finalized connection base URL must append `/` and the sub-route (e.g., `https://my.server.com/login`) when making Ktor network requests.

### B. Session & Token Interception
1. **Secure Storage**: Upon successful login, the client must store the returned session token, server URL, and username securely in `PreferencesManager`.
2. **Ktor Authorization Interception**: All outgoing HTTP requests handled by the Ktor HttpClient must be intercepted via Ktor client plugins to append the authorization header:
   `Authorization: Bearer <stored_token>`
   *Note: If no token is present, the interceptor must not append the header.*

### C. Error Mapping
1. **HTTP 401 Unauthorized**: If the server returns a 401 status during login, the client must catch it and throw a clean, user-friendly exception with the message:
   `Invalid username or password`
2. **General Errors**: Connection timeouts or offline conditions must be handled gracefully, mapping connection failures to readable notifications.
