import { useEffect } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { restoreSession, useAuth } from './lib/auth'
import { LoginPage } from './features/auth/LoginPage'
import { HomePlaceholder } from './features/home/HomePlaceholder'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
})

function AppRoutes() {
  const auth = useAuth()

  useEffect(() => {
    if (auth.status === 'loading') void restoreSession()
  }, [auth.status])

  // 저장된 토큰 확인 중에는 깜빡임을 막기 위해 아무것도 그리지 않는다 (0.1초 내외)
  if (auth.status === 'loading') return null

  return (
    <Routes>
      <Route
        path="/login"
        element={auth.status === 'authenticated' ? <Navigate to="/" replace /> : <LoginPage />}
      />
      <Route
        path="/"
        element={
          auth.status === 'authenticated' ? <HomePlaceholder me={auth.me} /> : <Navigate to="/login" replace />
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </QueryClientProvider>
  )
}
