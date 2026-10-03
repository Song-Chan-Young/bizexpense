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

로컬 프로필은 `./.data/` 아래 H2 파일 DB를 사용하고, 처음 실행할 때 테스트 계정을 만든다.

| 아이디 | 권한 | 부서 | 비밀번호 |
|---|---|---|---|
| admin | 관리자 | 경영지원팀 | pass1234 |
| manager1 | 팀장 | 영업1팀 | pass1234 |
| user1, user2 | 일반 직원 | 영업1팀 | pass1234 |

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
- [ ] Phase 5 - 경비
- [ ] Phase 6 - 파일
- [ ] Phase 7 - 결재 (공통 결재 구조와 출장 결재는 완료, 정산 결재 연결 예정)
- [ ] Phase 8 - 정산
- [ ] Phase 9 - 대시보드
- [ ] Phase 10 - 완성도
