import styles from './ShiftStrip.module.css'

type Cell = 'open' | 'close' | 'off'

/** 로고 자리에 쓰는 정체성 요소. 한 주의 근무표를 7칸으로 축약했다. */
const WEEK: Cell[] = ['open', 'open', 'off', 'close', 'close', 'off', 'off']

export function ShiftStrip({ className, size = 'md' }: { className?: string; size?: 'md' | 'lg' }) {
  return (
    <div className={[styles.strip, styles[size], className].filter(Boolean).join(' ')} aria-hidden="true">
      {WEEK.map((cell, i) => (
        <span key={i} className={`${styles.cell} ${styles[cell]}`} />
      ))}
    </div>
  )
}
