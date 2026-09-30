# Shiftly

> 소규모 매장·팀을 위한 근무 스케줄 관리 & 급여 정산 서비스

관리자는 주간 근무표를 짜고, 직원은 출퇴근을 기록하며, 주 52시간 규칙 위반을 사전에 막고 월말 급여를 자동으로 계산합니다.

<!-- TODO: 대시보드 + 모바일 출퇴근 화면 스크린샷 한 장 -->

## 바로 체험하기

| 구분 | 링크 |
|---|---|
| 서비스 | https://shiftly.example.com <!-- TODO --> |
| API 문서 (Swagger) | https://api.shiftly.example.com/swagger-ui.html <!-- TODO --> |

로그인 화면의 **"테스트 계정으로 체험하기"** 버튼으로 회원가입 없이 관리자/직원 화면을 볼 수 있습니다.

| 역할 | 볼 수 있는 것 |
|---|---|
| 관리자 | 대시보드, 근무표 편집(52시간 검증), 직원 관리, 월 정산 |
| 직원 | 출퇴근 기록, 내 근무표, 주간 근무시간 |

---

## 주요 기능

- **근무표 편집**: 직원 × 요일 매트릭스에서 근무 유형 배정. 배정 즉시 주간 합계 갱신, 52시간 초과 시 경고 및 저장 차단
- **출퇴근 기록**: 모바일 웹에서 원터치 출근/퇴근. 퇴근 시 근무·연장·야간 시간 자동 계산
- **관리자 대시보드**: 오늘의 출근/지각/미출근 현황, 52시간 임박 직원 알림
- **급여 정산** *(2단계)*: 월 마감 시 기본급·연장·야간·휴일 수당 계산, 급여명세서 PDF 발급

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Java 17, Spring Boot 4.x, Spring Security (JWT), Spring Data JPA, QueryDSL |
| Database | PostgreSQL 16, Redis (리프레시 토큰) |
| Frontend | React 18, TypeScript, Vite, TanStack Query, Zustand |
| Infra | Docker, GitHub Actions, AWS (EC2, RDS) <!-- TODO: 실제 배포 환경으로 수정 --> |
| Docs | OpenAPI 3 (Swagger), openapi-typescript로 프론트 타입 자동 생성 |

---

## 아키텍처

```
┌─────────────┐      ┌──────────────────┐      ┌───────────┐
│   Browser   │ ───▶ │  React (Vercel)   │ ───▶ │           │
│  (PC/Mobile)│      └──────────────────┘      │  Spring   │      ┌─────────┐
└─────────────┘                                │  Boot API │ ───▶ │Postgres │
                                               │  (AWS)    │      └─────────┘
                                               │           │ ───▶ ┌─────────┐
                                               └───────────┘      │  Redis  │
                                                                  └─────────┘
```

<!-- TODO: 실제 배포 구성 확정 후 다이어그램 이미지로 교체 -->

### 백엔드 패키지 구조

```
com.shiftly
├── global/          # 공통: 설정, 예외 처리, 응답 포맷, 보안
├── domain/
│   ├── auth/        # 로그인, 토큰
│   ├── user/        # 직원, 조직
│   ├── schedule/    # 근무 유형, 근무표
│   ├── attendance/  # 출퇴근, 근무시간 계산
│   └── payroll/     # 정산 (2단계)
└── ShiftlyApplication.java
```

각 도메인은 `controller / service / repository / entity / dto` 로 구성됩니다.

---

## 주요 설계 결정

자세한 내용은 [docs/adr](./docs/adr) 참고.

1. **계획(schedule)과 실적(attendance)을 별도 테이블로 분리**
   근무표는 관리자가 주 단위로 편집하고, 출퇴근은 직원이 실시간으로 기록합니다. 수정 주체와 주기가 달라 분리했습니다.

2. **근무·연장·야간 시간은 퇴근 시점에 계산해서 저장**
   조회마다 재계산하지 않기 위해 스냅샷으로 저장합니다. 규칙 변경 시에는 재계산 배치로 대응합니다.

3. **주간 근무표 저장은 기간 전체 덮어쓰기(PUT) 방식**
   셀 단위 부분 수정 API 대신 주간 단위로 일괄 저장합니다. 52시간 검증을 한 트랜잭션에서 처리하기 위함입니다.

4. **52시간 검증은 프론트와 백엔드 양쪽에서 수행**
   프론트는 즉각 피드백용, 백엔드는 최종 판정. 저장 전 `/validate` API로 두 결과를 대조합니다.

---

## 테스트

```bash
cd backend && ./gradlew test
```

핵심 테스트 대상은 근무시간 계산 로직입니다.

| 케이스 | 기대 결과 |
|---|---|
| 09:00~18:00, 휴게 1h | 근무 8h, 연장 0, 야간 0 |
| 14:00~23:00 마감조 | 야간 1h (22~23시) |
| 22:00~07:00 자정 넘김 | 야간 8h |
| 예정 18:00, 실제 20:00 퇴근 | 연장 2h |

<!-- TODO: 커버리지 뱃지 -->

---

## 로컬 실행

### 요구사항

- Java 17, Node 22+, Docker

### 실행

```bash
# 1. DB 실행
docker compose up -d

# 2. 백엔드
cd backend
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 프론트
cd frontend
cp .env.example .env.local
npm install && npm run dev
```

- 백엔드: http://localhost:8080 (Swagger: /swagger-ui.html)
- 프론트: http://localhost:5173

초기 데이터(테스트 조직, 관리자 1명, 직원 5명, 2주치 근무표)는 `local` 프로파일 실행 시 자동으로 들어갑니다.

---

## 문서

- [화면 구성](./docs/screens.md)
- [ERD & API 목록](./docs/erd-api.md)
- [설계 결정 기록 (ADR)](./docs/adr)

---

## 개발 기록

<!-- TODO: 진행하면서 채우기. 예시: -->
<!-- - 2026.10 – MVP 배포 (로그인, 출퇴근, 근무표 조회) -->
<!-- - 2026.11 – 근무표 편집 + 52시간 검증 -->
<!-- - 2026.12 – 월 정산 배치, PDF 발급 -->
<!-- - 2027.01 – 마감 배치 부하 테스트 및 개선 (N+1 제거로 3.2s → 0.4s) -->

---

## 라이선스

MIT
