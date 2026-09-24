const API_BASE = "/api/v1";

export class ApiError extends Error {
  constructor(
    public status: number,
    public title: string,
    message: string,
    public errors?: Record<string, string>,
  ) {
    super(message);
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (init.body) {
    headers.set("Content-Type", "application/json");
  }
  if (method !== "GET" && method !== "HEAD") {
    let xsrfToken = readCookie("XSRF-TOKEN");
    if (!xsrfToken) {
      await fetch(`${API_BASE}/auth/csrf`, { credentials: "include" });
      xsrfToken = readCookie("XSRF-TOKEN");
    }
    if (xsrfToken) {
      headers.set("X-XSRF-TOKEN", xsrfToken);
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    method,
    headers,
    credentials: "include",
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const isJson = response.headers.get("content-type")?.includes("json");
  const body = isJson ? await response.json() : undefined;

  if (!response.ok) {
    throw new ApiError(
      response.status,
      body?.title ?? "Erro",
      body?.detail ?? "Ocorreu um erro inesperado.",
      body?.errors,
    );
  }

  return body as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "POST", body: data ? JSON.stringify(data) : undefined }),
  put: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "PUT", body: data ? JSON.stringify(data) : undefined }),
  del: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
