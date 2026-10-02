import { useSyncExternalStore } from 'react'
import { api, tokenStorage } from './api'

export type Role = 'ADMIN' | 'EMPLOYEE'

export interface Me {
  id: number
  email: string
  name: string
  role: Role
  organization: { id: number; name: string }
}

interface TokenResponse {
  accessToken: string
  refreshToken: string
  accessTokenExpiresIn: number
}

/** 앱 전역 인증 상태. 외부 스토어 + useSyncExternalStore 로 라이브러리 없이 구독한다. */
type AuthState =
  | { status: 'loading' }
  | { status: 'anonymous' }
  | { status: 'authenticated'; me: Me }

let state: AuthState = tokenStorage.access ? { status: 'loading' } : { status: 'anonymous' }
const listeners = new Set<() => void>()

function setState(next: AuthState) {
  state = next
  listeners.forEach((l) => l())
}

export function useAuth(): AuthState {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    () => state,
  )
}

/** 앱 시작 시 저장된 토큰으로 내 정보를 복원한다 */
export async function restoreSession(): Promise<void> {
  if (!tokenStorage.access) {
    setState({ status: 'anonymous' })
    return
  }
  try {
    const me = await api<Me>('/auth/me')
    setState({ status: 'authenticated', me })
  } catch {
    tokenStorage.clear()
    setState({ status: 'anonymous' })
  }
}

async function completeLogin(tokens: TokenResponse): Promise<Me> {
  tokenStorage.set(tokens.accessToken, tokens.refreshToken)
  const me = await api<Me>('/auth/me')
  setState({ status: 'authenticated', me })
  return me
}

export async function login(email: string, password: string): Promise<Me> {
  const tokens = await api<TokenResponse>('/auth/login', {
    method: 'POST',
    body: { email, password },
    anonymous: true,
  })
  return completeLogin(tokens)
}

export async function demoLogin(role: Role): Promise<Me> {
  const tokens = await api<TokenResponse>('/auth/demo-login', {
    method: 'POST',
    body: { role },
    anonymous: true,
  })
  return completeLogin(tokens)
}

export async function logout(): Promise<void> {
  const refresh = tokenStorage.refresh
  tokenStorage.clear()
  setState({ status: 'anonymous' })
  if (refresh) {
    // 실패해도 로컬은 이미 정리됐으니 무시
    await api('/auth/logout', { method: 'POST', body: { refreshToken: refresh }, anonymous: true }).catch(
      () => undefined,
    )
  }
}
