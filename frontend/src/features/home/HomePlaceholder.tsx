import { useNavigate } from 'react-router'
import { logout, type Me } from '../../lib/auth'
import { Button } from '../../components/Button'
import { ShiftStrip } from '../../components/ShiftStrip'
import styles from './HomePlaceholder.module.css'

/** 로그인 흐름 확인용. 직원 홈/관리자 대시보드가 생기면 삭제한다. */
export function HomePlaceholder({ me }: { me: Me }) {
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <main className={styles.page}>
      <div className={styles.panel}>
        <ShiftStrip />
        <h1 className={styles.title}>{me.name}님, 로그인했습니다</h1>
        <dl className={styles.meta}>
          <dt>이메일</dt>
          <dd>{me.email}</dd>
          <dt>역할</dt>
          <dd>{me.role === 'ADMIN' ? '관리자' : '직원'}</dd>
          <dt>소속</dt>
          <dd>{me.organization.name}</dd>
        </dl>
        <p className={styles.note}>
          {me.role === 'ADMIN' ? '관리자 대시보드' : '출퇴근 화면'}는 다음 작업에서 이 자리에 들어옵니다.
        </p>
        <Button variant="outline" type="button" onClick={handleLogout} className={styles.logout}>
          로그아웃
        </Button>
      </div>
    </main>
  )
}
