# Shiftly – ERD & API 목록 (MVP)

`shiftly-screens.md`의 화면 5개를 기준으로 뽑은 데이터 모델과 API.
2단계(정산, 직원관리) 관련 항목은 별도 표시.

---

## 1. ERD

### 테이블 관계

```
organization 1 ──< users
organization 1 ──< shift_type
users        1 ──< schedule >── 1 shift_type
users        1 ──< attendance
schedule     1 ──  0..1 attendance   (해당 날짜 근무에 대한 실제 출퇴근)
```

### dbdiagram.io 용 DBML

```dbml
Table organization {
  id          bigint [pk, increment]
  name        varchar(100) [not null]        // 매장 A
  timezone    varchar(50)  [default: 'Asia/Seoul']
  created_at  timestamp
  updated_at  timestamp
}

Table users {
  id              bigint [pk, increment]
  organization_id bigint [not null, ref: > organization.id]
  email           varchar(255) [not null, unique]
  password_hash   varchar(255) [not null]
  name            varchar(50)  [not null]
  role            varchar(20)  [not null]    // ADMIN / EMPLOYEE
  hourly_wage     int                        // 2단계: 시급 (원)
  status          varchar(20)  [default: 'ACTIVE']  // ACTIVE / INACTIVE
  created_at      timestamp
  updated_at      timestamp
}

Table shift_type {
  id              bigint [pk, increment]
  organization_id bigint [not null, ref: > organization.id]
  name            varchar(30) [not null]     // 오픈, 마감
  short_name      varchar(5)  [not null]     // 오, 마
  start_time      time [not null]            // 09:00
  end_time        time [not null]            // 18:00  (자정 넘김 허용: end < start)
  break_minutes   int  [default: 60]
  color           varchar(7)                 // #4A90E2
  created_at      timestamp
}

Table schedule {
  id            bigint [pk, increment]
  user_id       bigint [not null, ref: > users.id]
  shift_type_id bigint [not null, ref: > shift_type.id]
  work_date     date   [not null]
  memo          varchar(200)
  created_by    bigint [ref: > users.id]
  created_at    timestamp
  updated_at    timestamp

  indexes {
    (user_id, work_date) [unique]            // 하루 한 근무만
    (work_date)
  }
}

Table attendance {
  id            bigint [pk, increment]
  user_id       bigint [not null, ref: > users.id]
  schedule_id   bigint [ref: > schedule.id]  // 스케줄 없는 출근도 허용(null)
  work_date     date   [not null]
  clock_in_at   timestamp
  clock_out_at  timestamp
  status        varchar(20) [not null]       // SCHEDULED / WORKING / DONE / ABSENT / LATE
  worked_minutes   int                       // 퇴근 시 계산
  overtime_minutes int                       // 퇴근 시 계산
  night_minutes    int                       // 22:00~06:00 구간
  created_at    timestamp
  updated_at    timestamp

  indexes {
    (user_id, work_date) [unique]
  }
}
```

### 설계 메모 (ADR 후보)

- **schedule과 attendance를 분리**한 이유: 계획(근무표)과 실적(출퇴근)은 수정 주기와 주체가 다름. 근무표는 관리자가 주 단위로 편집, 출퇴근은 직원이 실시간으로 기록.
- **worked/overtime/night_minutes를 퇴근 시 계산해서 저장**: 조회마다 재계산하지 않기 위함. 규칙이 바뀌면 재계산 배치 필요(2단계 정산에서 다룸).
- **shift_type의 end_time < start_time 허용**: 마감조가 자정을 넘기는 경우. 야간 시간 계산의 핵심 경계 케이스.
- **테이블명 `users`**: PostgreSQL에서 `user`는 예약어라 복수형 사용. JPA 엔티티는 `User`, `@Table(name = "users")`.
- **주 52시간 집계는 테이블 없이 계산**: schedule(계획) 기준으로 프론트/백엔드 모두 계산. 캐시가 필요해지면 그때 추가.

### 2단계에서 추가될 테이블

| 테이블 | 용도 |
|---|---|
| refresh_token | JWT 리프레시 토큰 회전 |
| payroll | 월별 직원 급여 정산 결과 (마감 후 스냅샷) |
| payroll_item | 기본급 / 연장 / 야간 / 휴일 항목별 금액 |
| holiday | 공휴일 (휴일 수당 판단) |

---

## 2. API 목록

- Base URL: `/api/v1`
- 인증: `Authorization: Bearer {accessToken}`
- 응답 공통 포맷:

```json
{ "success": true, "data": { }, "error": null }
{ "success": false, "data": null, "error": { "code": "SCHEDULE_WEEKLY_LIMIT_EXCEEDED", "message": "..." } }
```

### 2-1. 인증 (화면 1)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | `/auth/login` | - | 이메일/비밀번호 로그인 → access/refresh 토큰 |
| POST | `/auth/refresh` | - | 리프레시 토큰으로 access 재발급 |
| POST | `/auth/logout` | 로그인 | 리프레시 토큰 폐기 |
| GET | `/auth/me` | 로그인 | 내 정보 (id, name, role, organization) |
| POST | `/auth/demo-login` | - | 테스트 계정 로그인 `{ role: "ADMIN" \| "EMPLOYEE" }` |

### 2-2. 출퇴근 (화면 2)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/attendances/today` | 직원 | 오늘 스케줄 + 출퇴근 상태 |
| POST | `/attendances/clock-in` | 직원 | 출근 기록. 이미 출근했으면 409 |
| POST | `/attendances/clock-out` | 직원 | 퇴근 기록. worked/overtime/night 계산해서 저장 |
| GET | `/attendances/weekly-summary?date=2026-09-22` | 직원 | 해당 주 근무시간 합계 (계획 + 실적, 연장, 야간) |

### 2-3. 근무표 – 직원 (화면 3)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/schedules/me?year=2026&month=9` | 직원 | 내 월간 근무표 |
| GET | `/schedules/me/{date}` | 직원 | 특정 날짜 상세 + 같은 조 동료 목록 |

### 2-4. 관리자 대시보드 (화면 4)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/admin/dashboard/summary?date=2026-09-22` | 관리자 | 카드 4개: 출근/지각/미출근/52h 임박 인원 |
| GET | `/admin/dashboard/attendances?date=2026-09-22` | 관리자 | 오늘 근무 현황 표 |
| GET | `/admin/dashboard/alerts?date=2026-09-22` | 관리자 | 주의 필요 목록 (52h 임박, 미출근) |

### 2-5. 근무표 편집 – 관리자 (화면 5)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/admin/shift-types` | 관리자 | 근무 유형 목록 (오픈/마감) |
| POST | `/admin/shift-types` | 관리자 | 근무 유형 생성 |
| PUT | `/admin/shift-types/{id}` | 관리자 | 근무 유형 수정 |
| GET | `/admin/schedules?from=2026-09-22&to=2026-09-28` | 관리자 | 주간 근무표 (직원 × 요일 매트릭스 + 주간 합계) |
| PUT | `/admin/schedules` | 관리자 | 주간 근무표 일괄 저장 (아래 상세) |
| POST | `/admin/schedules/validate` | 관리자 | 저장 없이 52h 규칙 검증만 (프론트 계산 결과 대조용) |
| GET | `/admin/users` | 관리자 | 직원 목록 (근무표 행 구성용, 2단계에서 CRUD 확장) |

#### PUT /admin/schedules 요청 예시

```json
{
  "from": "2026-09-22",
  "to": "2026-09-28",
  "items": [
    { "userId": 1, "workDate": "2026-09-22", "shiftTypeId": 1 },
    { "userId": 1, "workDate": "2026-09-23", "shiftTypeId": 1 },
    { "userId": 1, "workDate": "2026-09-24", "shiftTypeId": null },
    { "userId": 3, "workDate": "2026-09-27", "shiftTypeId": 1 }
  ]
}
```

- `shiftTypeId: null` → 해당 날짜 스케줄 삭제(휴무)
- 기간 내 전체를 덮어쓰는 방식(upsert + delete). 부분 수정 API는 만들지 않음
- 저장 전 서버에서 직원별 주간 합계 검증. 초과 시 `422` + 어떤 직원이 몇 시간인지 응답:

```json
{
  "success": false,
  "error": {
    "code": "SCHEDULE_WEEKLY_LIMIT_EXCEEDED",
    "message": "주 52시간을 초과하는 직원이 있습니다.",
    "details": [
      { "userId": 3, "userName": "최민수", "weeklyHours": 57 }
    ]
  }
}
```

### 2-6. 2단계 API (MVP 제외)

| Method | Path | 설명 |
|---|---|---|
| GET | `/attendances/me?year=&month=` | 내 월별 근무기록 |
| POST/PUT/DELETE | `/admin/users` | 직원 CRUD, 시급 설정 |
| POST | `/admin/payrolls/close?year=&month=` | 월 마감 → 급여 계산 배치 |
| GET | `/admin/payrolls?year=&month=` | 직원별 정산 결과 |
| GET | `/admin/payrolls/{id}/pdf` | 급여명세서 PDF |

---

## 3. 에러 코드 초안

| 코드 | HTTP | 상황 |
|---|---|---|
| AUTH_INVALID_CREDENTIALS | 401 | 이메일/비번 불일치 |
| AUTH_TOKEN_EXPIRED | 401 | 토큰 만료 |
| FORBIDDEN | 403 | 권한 없음 (직원이 admin API 호출) |
| ATTENDANCE_ALREADY_CLOCKED_IN | 409 | 중복 출근 |
| ATTENDANCE_NOT_CLOCKED_IN | 409 | 출근 없이 퇴근 |
| SCHEDULE_WEEKLY_LIMIT_EXCEEDED | 422 | 주 52시간 초과 |
| SCHEDULE_DUPLICATE_DATE | 422 | 같은 날 중복 배정 |

---

## 4. 구현 순서 제안

1. organization, user 테이블 + `/auth/*` → 로그인 화면 연결
2. shift_type, schedule 테이블 + `/schedules/me` → 내 근무표 화면
3. attendance 테이블 + `/attendances/*` → 출퇴근 화면
4. `/admin/dashboard/*` → 대시보드
5. `/admin/schedules` GET/PUT/validate → 근무표 편집

**테스트 먼저 쓸 것**: 퇴근 시 worked/overtime/night 계산 로직. 케이스는 최소
- 09:00~18:00 일반 근무 (휴게 1h → 8h)
- 14:00~23:00 마감 (야간 1h)
- 22:00~07:00 자정 넘김 (야간 8h)
- 예정보다 2시간 늦게 퇴근 (연장 2h)
