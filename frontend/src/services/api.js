import { getToken } from "./auth";

export async function apiFetch(path, {method = "GET", body, headers = {} } = {}){
    const token = getToken();

    const res = await fetch(path, {
        method,
        headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            headers,
        },
        body: body ? JSON.stringify(body) : undefined,
    });

    const text = await res.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = text;
    }

    if (!res.ok) {
        const msg =
            typeof data === "string"
                ? data
                : (data?.message || data?.error || `HTTP ${res.status}`);
        throw new Error(msg);
    }

    return data;
}


