# Bridge (Kotlin → React)

## Design

- **WebViewBridge**: Facade over `UserRepository`, `BiometricPrefs`, `BiometricCredentialStore`. Single entry point for JS; no raw JSON building.
- **BridgeJson**: Single place for all bridge response JSON (contract with React). Use `BridgeJson.success()`, `BridgeJson.error(msg)`, `BridgeJson.userResult(...)`, etc. so responses stay consistent and type-safe.
- **userFacingMessage(Throwable)**: One place for Persian user-facing error messages from exceptions.

## Flow

Kotlin (data) → WebViewBridge (facade) → JSON string → React `androidBridge.ts` (parse) → AuthContext / pages.
