# BizExpense

기업 출장·일정·경비·결재·정산 관리 시스템. 이직용 포트폴리오 프로젝트이며, 원 설계서(MyBatis/MariaDB/Java 17)와 달리
**Spring Boot 4 + JPA + Java 21 + React** 로 구현한다. 규칙과 상태 흐름은 `README.md` 에 정리되어 있다.

## 실행

- JDK 21 필요. 기본 `java` 가 26 이면 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` 후 실행.
- 백엔드: `./gradlew bootRun` (http://localhost:8080, 기본 `local` 프로필, H2 파일 DB `./.data/`)
- 프론트: `cd frontend && npm run dev` (http://localhost:5173, `/api` 는 8080 으로 프록시)
- 테스트: `./gradlew test` (현재 108개). 프론트: `npm run build && npm run lint`
- 로컬 계정: admin / manager1 / user1 / user2 / manager2 / user3, 비밀번호 `pass1234`

## 구조

- `src/main/java/com/bizexpense/domain/<도메인>` — auth, user, department, schedule, trip, expense, code(비용 항목·결제 수단), approval, settlement
- `src/main/java/com/bizexpense/global` — 공통 응답(`ApiResponse`), `ErrorCode`/`BusinessException`/전역 예외 처리, JWT 보안, 설정, 데모 데이터
- `frontend/src/pages/<화면>`, `frontend/src/api/<도메인>.js`, `frontend/src/components` (공통)

## 구현 관례

- 상태 전이 규칙은 엔티티 메서드에서 검증한다 (`Trip.request()`, `Expense.requestSettlement()` 등). 위반 시 `BusinessException(ErrorCode)`.
- 접근 권한: 조회는 본인·같은 부서 팀장·관리자, 변경은 본인만 → `AccessPolicy` 사용. 목록 범위는 `ViewScope`(ME/TEAM/ALL).
- 결재는 공통 `Approval`(대상 종류 + 대상 ID). 대상별 결과 반영은 `ApprovalHandler` 구현체. 처리기는 순환 참조를 피하려고 서비스 대신 저장소를 직접 쓴다.
- 검색은 `Specification`, 목록 응답은 `PageResponse`.
- 프론트 비동기 조회는 `ignore` 플래그로 늦게 온 응답을 버린다. 화면 주소를 추가하면 `SpaForwardingController` 에도 추가한다.
- 주석·커밋 메시지는 한국어. 커밋 메시지는 `feat:` / `fix:` 등 접두어.

## 브랜치 / 배포

- `feature/*` → `develop` (--no-ff 병합) → `main`. **`main` 에 push 하면 Render 가 자동 배포**한다.
- 배포 주소: https://bizexpense-73h9.onrender.com (Docker, 기본 프로필 `demo,standalone`: 메모리 DB, 재시작 시 데모 데이터로 초기화)
- 배포 사이트 비밀번호는 **`demo1234`**, 첫 접속 시 관리자 자동 로그인(로그아웃하면 그 브라우저에서는 끔).
- 브라우저 E2E 확인용 스크립트는 `.e2e/` (git 제외, playwright-core + 설치된 Chrome).

## 진행 상황 / 다음 작업

- 완료: Phase 1~5, 7, 8 (기본 구조, 로그인/권한, 출장, 일정, 경비, 결재, 정산), 배포, 모바일 화면
- 남음: Phase 6 영수증 첨부(경비 `proof` 필드는 준비됨), Phase 9 대시보드 통계(대시보드 카드는 아직 `–`), Phase 10 감사 로그·API 문서 등
