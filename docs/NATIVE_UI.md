# Native UI redesign and X authentication

## What changed

- All tabs now use a cohesive Material 3 dark theme, refined navigation and native controls.
- Home timeline renders actual Coil media thumbnails instead of text-only counters.
- Favorites preserve existing Room data and show thumbnails when available.
- Explore supports author follow/unfollow and pasting a post URL.
- Downloads show progress, result state and retry/cancel controls.
- The embedded X login WebView and its CookieManager operations have been removed.
- Existing stored auth_token/ct0 are **retained** through this UI migration, with no Room schema changes.

## Current login limitations

This application still reads media through unofficial, cookie-authenticated X web APIs in `XApiClient.kt`. The native login page therefore **does not** ask for an X password, impersonate the X login form, or inspect cookies in a third-party browser. The current compatibility flow requires the user to enter their existing `auth_token` and `ct0` session values. It validates the values before saving them in the existing EncryptedSharedPreferences. The optional "Open X in external browser" action is deliberately NOT described as auto-login: a browser tab cannot expose cookies to the native app.

WebView was removed because embedded browser sign-in is not the ideal solution, but this does not magically turn unofficial X endpoints into OAuth-compatible endpoints. If X changes its API or session policy, reading media can still fail.

## Long-term solution: official X OAuth 2.0 + PKCE

1. Register an Android/public OAuth client with X Developer Console and configure an exact callback URL / verified app link.
2. Use Android browser-mediated authentication (Chrome Auth Tab / Custom Tabs or AppAuth); never embed an X password form or ship a client secret in the APK.
3. Validate OAuth `state`, `code_verifier` and redirect, exchange the authorization code for short-lived OAuth access tokens, and rotate refresh tokens using `offline.access` where supported.
4. Replace `XApiClient.kt`'s unofficial GraphQL and cookie-based calls with the X API v2 users/posts/media lookup endpoints and scopes (`tweet.read`, `users.read`; additional scopes only if required).
5. Update the parser and media downloader for the official API media data model and separately verify video accessibility and API quotas.
6. Check the current X API pay-per-use price, developer app access and budget before enabling production traffic.
7. Only remove encrypted legacy session storage after data migration / explicit user logout.

**Actions signing keystore secrets do not equal X OAuth Client ID.** This migration is intentionally not enabled without credentials or API access, so the existing downloader remains functional for users whose cookie-authenticated API requests still work.

## Verification

Run `bash ./gradlew testDebugUnitTest assembleDebug` on PR, then verify installed APK navigation and login flow on device. CI publishes a signed release from master only after tests pass.
