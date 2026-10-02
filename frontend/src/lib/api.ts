/** 백엔드 공통 응답 포맷 (ApiResponse.java 와 1:1) */
export interface ApiErrorBody {
  code: string
  message: string
  details: unknown
}

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: ApiErrorBody | null
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: unknown

  constructor(status: number, body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = status
    this.code = body.code
    this.details = body.details
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const tokenStorage = {
  get access() {
    return localStorage.getItem('shiftly.access')
  },
  get refresh() {
    return localStorage.getItem('shiftly.refresh')
  },
  set(access: string, refresh: string) {
    localStorage.setItem('shiftly.access', access)
    localStorage.setItem('shiftly.refresh', refresh)
  },
  clear() {
    localStorage.removeItem('shiftly.access')
    localStorage.removeItem('shiftly.refresh')
  },
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  /** 토큰을 붙이지 않는다 (로그인 등) */
  anonymous?: boolean
}

async function rawRequest<T>(path: string, options: RequestOptions): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (!options.anonymous && tokenStorage.access) {
    headers.Authorization = `Bearer ${tokenStorage.access}`
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null

  if (!response.ok || !payload?.success) {
    const body = payload?.error ?? {
      code: 'NETWORK_ERROR',
      message: '서버에 연결할 수 없습니다.',
      details: null,
    }
    throw new ApiError(response.status, body)
  }
  return payload.data as T
}

let refreshing: Promise<boolean> | null = null

/** access 만료 시 refresh 로 한 번 재발급. 동시에 여러 요청이 와도 재발급은 한 번만. */
async function tryRefresh(): Promise<boolean> {
  if (!refreshing) {
    refreshing = (async () => {
      const refresh = tokenStorage.refresh
      if (!refresh) return false
      try {
        const tokens = await rawRequest<{ accessToken: string; refreshToken: string }>('/auth/refresh', {
          method: 'POST',
          body: { refreshToken: refresh },
          anonymous: true,
        })
        tokenStorage.set(tokens.accessToken, tokens.refreshToken)
        return true
      } catch {
        tokenStorage.clear()
        return false
      } finally {
        refreshing = null
      }
    })()
  }
  return refreshing
}

/**
 * API 호출 진입점.
 * 401 AUTH_TOKEN_EXPIRED 면 refresh 후 원래 요청을 한 번 다시 보낸다.
 */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  try {
    return await rawRequest<T>(path, options)
  } catch (e) {
    if (e instanceof ApiError && e.code === 'AUTH_TOKEN_EXPIRED' && !options.anonymous) {
      if (await tryRefresh()) {
        return rawRequest<T>(path, options)
      }
    }
    throw e
  }
}
