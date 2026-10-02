import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '../../lib/api'
import { demoLogin, login, type Role } from '../../lib/auth'
import { Button } from '../../components/Button'
import { Field } from '../../components/Field'
import { ShiftStrip } from '../../components/ShiftStrip'
import styles from './LoginPage.module.css'

type Pending = 'none' | 'login' | Role

export function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState<Pending>('none')
  const [formError, setFormError] = useState<string | null>(null)

  const busy = pending !== 'none'

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (busy) return
    setFormError(null)
    setPending('login')
    try {
      await login(email.trim(), password)
      navigate('/', { replace: true })
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : '로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.')
      setPending('none')
    }
  }

  async function handleDemo(role: Role) {
    if (busy) return
    setFormError(null)
    setPending(role)
    try {
      await demoLogin(role)
      navigate('/', { replace: true })
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : '체험 계정에 연결하지 못했습니다.')
      setPending('none')
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.panel}>
        <header className={styles.header}>
          <ShiftStrip size="lg" />
          <h1 className={styles.title}>Shiftly</h1>
          <p className={styles.subtitle}>근무표와 급여를 한 곳에서</p>
        </header>

        <form className={styles.form} onSubmit={handleSubmit} noValidate>
          <Field
            id="email"
            label="이메일"
            type="email"
            name="email"
            autoComplete="username"
            inputMode="email"
            placeholder="name@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Field
            id="password"
            label="비밀번호"
            type="password"
            name="password"
            autoComplete="current-password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <div className={styles.status} role="alert" aria-live="polite">
            {formError}
          </div>

          <Button type="submit" loading={pending === 'login'} disabled={busy || !email || !password}>
            {pending === 'login' ? '확인하는 중' : '로그인'}
          </Button>
        </form>

        <div className={styles.divider} role="separator">
          <span>처음이신가요</span>
        </div>

        <div className={styles.demo}>
          <Button variant="outline" type="button" loading={pending === 'ADMIN'} disabled={busy} onClick={() => handleDemo('ADMIN')}>
            관리자로 둘러보기
          </Button>
          <Button variant="outline" type="button" loading={pending === 'EMPLOYEE'} disabled={busy} onClick={() => handleDemo('EMPLOYEE')}>
            직원으로 둘러보기
          </Button>
          <p className={styles.demoHint}>가입 없이 미리 만들어 둔 계정으로 들어갑니다.</p>
        </div>
      </div>
    </main>
  )
}
