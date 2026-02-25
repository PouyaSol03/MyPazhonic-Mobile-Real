# Android Bridge for React (WebView)

When the app starts, a **splash screen** is shown (Pazhonic logo + wordmark). The WebView loads the React app in the background. The splash is hidden **automatically when the web page has finished loading** (`onPageFinished`). Optionally, React can hide it earlier by signalling ready.

## Hiding splash (optional for React)

The splash disappears when the WebView finishes loading the page. To hide it **earlier** (e.g. when your root component has mounted), you can:

**Option 1:** Dispatch the custom event:

```javascript
window.dispatchEvent(new Event('react-ready'));
```

**Option 2:** Call the bridge directly:

```javascript
if (window.AndroidBridge && typeof window.AndroidBridge.onReactReady === 'function') {
  window.AndroidBridge.onReactReady();
}
```

If you do nothing, the splash still hides when the page load completes. A 10-second fallback exists only if the load never completes (e.g. error).

---

## Token-based auth flow

Auth is **session/token-based**:

1. **Register** – Create user (no login). Required: `userName`, `phoneNumber`, `password`.
2. **Login** – Validate phone + password; native generates a **session token**, stores it, and returns `{ success, token, user }`. Use the token in your React state (e.g. context/localStorage) for “logged in” UI.
3. **getLatestUser()** – Returns the current user **only if there is a valid session** (user has logged in). Returns `{ user: null }` when not logged in.
4. **getSessionToken()** – Returns the current session token (or `null`). Useful to restore “logged in” state on app load.
5. **logout()** – Clears the session token and local user data.

The native side stores the token; you do not send it back on every bridge call. Use `getLatestUser()` and `getSessionToken()` to check auth state.

---

## JavaScript bridge API (`window.AndroidBridge`)

All methods are synchronous from JS (native may run async work and return when done).

| Method | Description |
|--------|-------------|
| `onReactReady()` | Tells the app that React is ready; the splash screen is hidden. |
| `registerUser(userJson)` | **Register** user. `userJson`: required `userName`, `phoneNumber`, `password`; optional `fullName`, `firstName`, `lastName`, `nationalCode`, `avatarUrl`, `ipAddress`. Returns `{"success":true}` or `{"success":false,"error":"..."}`. Does not log in. |
| `login(phoneNumber, password)` | **Login**. On success creates session, returns `{"success":true,"token":"...","user":{...}}`. On failure `{"success":false,"error":"..."}`. Store `token` in React state if needed. |
| `getSessionToken()` | Current session token. Returns `{"token":"..."}` or `{"token":null}`. |
| `getLatestUser()` | Current user **only if session is valid**. Returns `{"user":{...}}` or `{"user":null}`. |
| `logout()` | Log out: clears session token and local user data. |
| `setBiometricEnabled("true" \| "false")` | Enable or disable biometric preference. |
| `getBiometricEnabled()` | Returns `"true"` or `"false"`. |

### Example (React) – register, login, token-based

```javascript
// Register (create account)
const reg = JSON.parse(window.AndroidBridge.registerUser(JSON.stringify({
  userName: 'johndoe',
  phoneNumber: '+989121234567',
  password: 'secret',
  fullName: 'John Doe',
})));
if (!reg.success) throw new Error(reg.error);

// Login (get token + user)
const login = JSON.parse(window.AndroidBridge.login('+989121234567', 'secret'));
if (!login.success) throw new Error(login.error);
const { token, user } = login;
// Store token in state/context/localStorage for “logged in” UI
localStorage.setItem('token', token);

// Get current user (only if session valid)
const { user: currentUser } = JSON.parse(window.AndroidBridge.getLatestUser());

// Get session token (e.g. on app load to restore auth state)
const { token: storedToken } = JSON.parse(window.AndroidBridge.getSessionToken());

// Logout
window.AndroidBridge.logout();
localStorage.removeItem('token');
```
