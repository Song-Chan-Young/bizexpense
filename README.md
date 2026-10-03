# BizExpense

기업 출장·경비·일정 관리 시스템. 출장 일정과 업무 일정을 등록하고, 출장 중 발생한 경비와 증빙을 관리한 뒤 결재와 정산까지 처리한다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Security, JWT(jjwt), Spring Data JPA, Bean Validation, Lombok |
| DB | PostgreSQL (운영), H2 PostgreSQL 호환 모드 (로컬) |
| Frontend | React 19, Vite, React Router, Axios |

## 실행 방법

**JDK 21이 필요하다.** Gradle 9.7 은 JDK 26 에서도 동작하지만 toolchain 은 21 을 사용한다.

```bash
# 백엔드 (http://localhost:8080, 기본 프로필 local)
./gradlew bootRun

# 프론트엔드 (http://localhost:5173, /api 는 8080 으로 프록시)
cd frontend
npm install
npm run dev
```

로컬 프로필은 `./.data/` 아래 H2 파일 DB를 사용하고, 처음 실행할 때 데모 계정과 예시 데이터(출장·일정·경비·결재)를 만든다.
로그인 화면의 체험 계정 버튼으로 바로 로그인할 수 있다.

| 아이디 | 권한 | 부서 | 비밀번호 (로컬) |
|---|---|---|---|
| admin | 관리자 | 경영지원팀 | pass1234 |
| manager1 | 팀장 | 영업1팀 | pass1234 |
| user1, user2 | 일반 직원 | 영업1팀 | pass1234 |
| manager2, user3 | 팀장 / 일반 직원 | 영업2팀 | pass1234 |

H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./.data/bizexpense`)

운영 프로필(`prod`)은 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`(Base64, 256bit 이상)을 사용한다.

## 출장 상태 흐름

```text
DRAFT(임시저장) ─신청─▶ REQUESTED(신청) ─승인─▶ APPROVED(승인) ─시작─▶ IN_PROGRESS(진행중) ─완료─▶ COMPLETED(완료)
     ▲                      │
     │                    반려 (사유 필수)
     │                      ▼
     └──── 수정 후 재신청 ── REJECTED(반려)

취소: DRAFT / REQUESTED / APPROVED 에서 가능 (대기 중 결재 건, 연결된 일정도 함께 취소)
삭제: DRAFT 만 가능 (연결된 일정은 남기고 연결만 해제)
```

결재자: 일반 직원 → 같은 부서 팀장(없으면 관리자), 팀장·관리자 → 다른 관리자.
결재 건은 신청할 때마다 새로 생기므로 반려 → 재신청 이력이 모두 남는다.

## 경비 규칙

- 출장이 시작된 뒤(진행중/완료)에만 본인 출장에 경비를 등록할 수 있다.
- 사용일은 출장 시작 30일 전(교통·숙박 사전 예매) ~ 출장 종료일까지 인정한다.
- 수정/삭제는 임시저장·반려 상태만 가능하다. 이후 상태는 정산(Phase 8) 흐름이 바꾼다.
- 결제 수단의 `법인 결제` 여부로 출장별 법인카드 합계와 개인 부담(정산 지급 예정)을 나눈다.
- 비용 항목/결제 수단은 삭제 대신 사용 중지한다 (기존 경비 보존). 초기 데이터는 모든 환경에서 자동 생성된다.

## 영수증 첨부

- 이미지(JPG, PNG, GIF, WEBP) 또는 PDF, 파일 1개 5MB, 경비 1건에 5개까지.
- 확장자와 파일 앞부분(매직 넘버)이 모두 맞아야 받는다. 브라우저가 보낸 Content-Type 은 쓰지 않는다.
- 첨부/삭제는 본인이 수정할 수 있는 상태(임시저장·반려)의 경비에만, 조회는 경비를 볼 수 있는 사람(본인·같은 부서 팀장·관리자)만.
- 파일이 1개 이상이면 경비의 증빙 여부(`proof_yn`)가 켜진다.
- 체험 서버는 재시작하면 디스크가 비워지므로 파일 내용도 DB(`bytea`)에 저장한다.

| 방식 | 주소 | 설명 |
| --- | --- | --- |
| GET | `/api/expenses/{id}/files` | 첨부 목록 (메타 정보) |
| POST | `/api/expenses/{id}/files` | 첨부 (`multipart/form-data`, `file`) |
| GET | `/api/expenses/{id}/files/{fileId}` | 파일 내용 (inline) |
| DELETE | `/api/expenses/{id}/files/{fileId}` | 삭제 |

## 정산 규칙

```text
총 경비 − 법인카드(회사 결제) = 개인 지급액

정산신청 ─팀장 승인─▶ 승인 ─관리자 지급 처리─▶ 정산완료
   │ ▲
 반려 │ 경비 수정·삭제·추가 후 재신청 (금액 다시 계산)
   ▼ │
  반려
```

- 완료된 본인 출장만 신청할 수 있고, 그 출장의 미정산 경비(임시저장)를 모두 묶는다.
- 출장당 진행 중인 정산은 하나만. 정산완료 후 추가된 경비는 새 정산으로 신청한다.
- 정산에 들어간 경비는 `settlement_id` 로 연결되어 다른 정산에 다시 들어갈 수 없고, 동시 요청은 낙관적 락(`@Version`)으로 막는다.
- 결재 승인/반려, 지급 처리는 정산 · 포함 경비 · 결재 건을 한 트랜잭션에서 함께 바꾼다.

## 배포 (Render)

React 빌드 결과를 Spring Boot 가 함께 서비스하는 **단일 Docker 이미지**로 배포한다 (`Dockerfile`).
`render.yaml`(Blueprint) 이 웹 서비스와 PostgreSQL 을 함께 만든다.

1. GitHub 에 저장소를 올린다 (`main` 브랜치).
2. [Render](https://render.com) → **New → Blueprint** → 저장소 선택 → **Apply**.
3. 빌드가 끝나면 `https://bizexpense-xxxx.onrender.com` 주소가 생긴다. 이 링크를 공유하면 PC·폰 어디서든 접속할 수 있다.

Docker 이미지 기본값은 `SPRING_PROFILES_ACTIVE=demo,standalone` 이다. 환경변수를 하나도 설정하지 않아도
메모리 DB + 데모 데이터로 바로 뜨며, 서버가 재시작되면 데모 데이터로 초기화된다 (체험 서버용).
PostgreSQL 에 데이터를 유지하려면 Blueprint(`render.yaml`)로 만들거나 아래 환경변수를 직접 설정한다.

| 환경변수 | 설명 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod,demo` (PostgreSQL + 데모 데이터). 실제 운영은 `prod` 만 |
| `JWT_SECRET` | Render 가 자동 생성 |
| `DEMO_PASSWORD` | 체험 계정 비밀번호 (기본 `demo1234`) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | Render PostgreSQL 에서 자동 연결 |

- 데모 데이터는 DB 에 사용자가 없을 때 한 번만, **처음 실행한 날 기준 날짜**로 만들어진다.
- 무료 플랜은 15분간 요청이 없으면 잠들어 첫 접속에 1분 정도 걸리고, 무료 PostgreSQL 은 30일 후 만료된다.
  계속 쓰려면 유료 플랜(웹 서비스 Starter, DB Basic)으로 바꾼다.

로컬에서 운영 모드 확인 (PostgreSQL 대신 H2):

```bash
cd frontend && npm ci && npm run build && cd ..
cp -R frontend/dist src/main/resources/static
./gradlew bootJar
SPRING_PROFILES_ACTIVE=prod,demo PORT=9090 JWT_SECRET=any-secret \
DB_URL='jdbc:h2:mem:prod;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE' DB_USERNAME=sa DB_PASSWORD= \
java -jar build/libs/app.jar
```

## 테스트

```bash
./gradlew test
```

## API 공통 응답

```json
{ "success": true, "data": { } }
{ "success": false, "error": { "code": "INVALID_CREDENTIALS", "message": "아이디 또는 비밀번호가 올바르지 않습니다." } }
```

## 진행 상황

- [x] Phase 1 - 기본 구조 (공통 응답, 예외 처리, 로그, 프로필 분리)
- [x] Phase 2 - 로그인 / 권한 (JWT, ROLE_USER / ROLE_MANAGER / ROLE_ADMIN)
- [x] Phase 3 - 출장 (CRUD, 상태 전이, 검색·페이징, 일정 연결) + 출장 결재(승인/반려/재신청)
- [x] Phase 4 - 일정 (CRUD, 월간/주간 캘린더, 목록 검색·페이징, 시간대 충돌 검사, 팀 일정 조회, 출장 연결)
- [x] Phase 5 - 경비 (비용 항목/결제 수단 관리, 경비 CRUD, 검색·페이징·합계, 출장별 경비 요약)
- [x] Phase 6 - 파일 (경비 영수증 첨부·미리보기·삭제, 형식·크기·개수 검증)
- [x] Phase 7 - 결재 (공통 결재 구조: 출장·정산 결재, 승인/반려/재신청, 결재함/내 신청/전체)
- [x] Phase 8 - 정산 (금액 계산, 신청/반려/재신청, 관리자 지급 처리, 중복 정산 방지)
- [ ] Phase 9 - 대시보드
- [ ] Phase 10 - 완성도
