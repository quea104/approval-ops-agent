const TOKEN_KEY = "aoa_token";
const USER_KEY = "aoa_token";

export function setAuth({ token, username }) {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    if (username) localStorage.setItem(USER_KEY, username);
}

export function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

export function getToken() {
    return localStorage.getItem(TOKEN_KEY) || "";
}

export function getUsername() {
    return localStorage.getItem(USER_KEY) || "guest";
}