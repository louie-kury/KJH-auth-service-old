# 인증 서비스 Swagger UI — 실행 계획서

- 상태: 실행 중. 사용자 지시에 따라 기존 #7 작업에서 진행하며 검증은 아직 완료되지 않았다.
- 대상: `../louie-kury-auth-service`, `feat/7-signup-api`. 기존 작업 및 이번 Swagger 관련 미커밋 변경을 보존한다.
- 작성일: 2026-11-25. 진행 근거: 기존 #7에서 진행하라는 사용자 지시. 별도의 계획·이슈 승인 여부는 기록되지 않았다.

## 목적과 범위

로컬 웹에서 현재 구현된 인증 API 5개를 살펴보고 직접 요청할 수 있게 한다. Swagger UI와 OpenAPI 문서, 해당 실행 안내 및 필요한 검증만 추가한다. 인증 정책·저장소·API 동작 변경이나 무관한 정비는 제외한다. 사용자 지시에 따라 기존 `#7` 가입 작업 브랜치에서 진행한다.

## 조사한 문서와 확인한 사실

| 자료·경로·절 | 적용 사실과 계획 반영 |
| --- | --- |
| `docs/ai/ROUTING.md` 읽기 규칙, `HUMAN_GATES.md` 일반 기능 개발, `WORKFLOW.md` 서비스 작업 경로 | 대상 서비스에서 실제 계약을 확인하고 계획 승인 → 이슈 작성·승인 → 브랜치 → 구현·검증 순서를 따른다. |
| `docs/prd.md` 6.1 ACC-01, 6.10, 10.1, 11, D11·D18, Q06·Q22 | 가입 전 이메일 확인, 이메일 유일성, 필수 가입 정보와 접근 보호가 기준이다. 문서 제공 기능은 기존 정책을 바꾸지 않는다. |
| `docs/erd.md` 기준과 표기, `docs/erd/svc_identity.md` account·login_credential·user_profile | MySQL 모델에서 account의 UUID·이메일은 유일하고 이름·전화번호·성별·생일·주소 등은 필수다. login_credential의 account_id·login_id와 user_profile의 account_id·nickname은 각각 유일하며 두 account_id는 account 참조 필수다. password_hash는 NULL 가능. 실제 DB 반영 여부는 미확인. 이 작업은 스키마를 바꾸지 않는다. |
| `docs/api/README.md` 공통 계약·멱등성, `docs/api/auth.md` 데이터 모델·이메일 인증과 일반 가입 | 성공 `data`, 오류 `code`·`message`; 가입의 이전 멱등 계약은 사용자 결정으로 제거됐다. 공통 문서 동기화는 별도 범위다. |
| 서비스 `AGENTS.md`, `build.gradle`, 컨트롤러 3개, `README.md` | Boot 4.1.2, Java 17, WebMVC. springdoc 3.x 호환은 메인이 확인한 사실이며 정확한 버전은 구현 시 호환되는 안정 버전으로 고정·검증한다. README의 `./gradlew test bootJar`, Windows ` .\gradlew.bat test bootJar` 및 `bootRun`을 따른다. |
| `docs/convension/BACKEND_CODE_CONVENTIONS.md` API·예외·테스트, `GIT_CONVENTIONS.md` 이슈·작업 브랜치 | 공통 응답과 실제 상태·헤더를 문서화하고 테스트와 실제 외부 연동을 구분한다. 이슈와 브랜치 이름은 해당 규칙을 따른다. |

프런트 화면·새 ERD 설계·학습 모드는 이번 범위가 아니므로 해당 자료는 적용하지 않는다.

## 제안하는 동작과 선택

현재 별도 API UI가 없다. 권장안은 springdoc WebMVC UI 3.x를 추가하고 로컬 프로필에서만 문서 엔드포인트를 활성화하는 것이다. 로컬에서 UI와 OpenAPI JSON에 접근해 다음 실제 API의 입력·성공·주요 오류를 확인하고 요청한다. 기본 및 배포 환경에서는 문서 노출을 끈다. 실제 인증 API의 권한·동작은 변경하지 않는다. 정확한 의존성 버전과 경로는 구현 중 확인한다.

| API | 입력 | 성공 응답 |
| --- | --- | --- |
| `POST /api/auth/email-verifications` | `email`, `purpose: SIGNUP` | 202 `data.verificationId`, `data.expiresAt` |
| `POST /api/auth/email-verifications/{verificationId}/confirm` | 경로 ID, 본문 `code` 6자리 문자열 | 200 `data.emailVerificationToken`, `data.expiresAt` |
| `GET /api/auth/login-ids/availability` | 필수 쿼리 `loginId`, 최대 100 코드 포인트 | 200 `data.available` |
| `GET /api/auth/nicknames/availability` | 필수 쿼리 `nickname`, 최대 30 코드 포인트 | 200 `data.available` |
| `POST /api/auth/signup` | 가입 필수 필드와 `emailVerificationToken` | 201 `data.memberId`, `Location: /api/members/me` |

가입의 모든 입력 필드·제한과 증명 소비 후 실패 시 재인증 안내, 각 API의 실제 오류 상태·본문을 코드와 서비스 README에 맞게 문서화한다. 인증번호·증명·비밀번호는 임의 예시 또는 자리표시자만 사용하고 실제 값은 문서·로그에 남기지 않는다. UI 요청은 실제 메일 발송·DB 쓰기를 유발할 수 있으므로 격리된 테스트 환경에서만 진행한다.

기존 충돌: PRD와 공통 auth 문서는 비밀번호 8~20자 조합 및 MongoDB 프로필을 요구하지만 서비스 README·코드는 15~128자와 MySQL 프로필을 사용한다. 공통 auth 문서의 확인 후 가입 소비 미구현 설명도 현재 코드와 다르다. 이번 UI는 실제 계약을 표시하고 정책을 확정하거나 공통 문서를 덮어쓰지 않는다. 메인이 별도 추적·판단한다.

## 진행 순서와 변경 파일

1. 사람이 이 계획을 검토·승인한다.
2. 메인이 승인된 범위의 이슈를 작성해 사람의 승인을 받는다. #7 연계 여부는 이때 확인한다.
3. 승인 후 현재 브랜치의 적합성을 판단하고 필요하면 컨벤션에 맞는 브랜치를 생성한다.
4. 구현 담당이 서비스 `build.gradle`, 로컬 프로필 설정, 웹 컨트롤러 또는 문서 설정과 `README.md`의 필요한 부분만 변경한다. 필요 시 OpenAPI 계약 검증 테스트를 추가한다. 서비스 외부 쓰기는 해당 환경의 권한 절차를 따른다.
5. `test bootJar`와 OpenAPI JSON의 5개 경로·헤더·요청/응답·오류 및 로컬 UI 접속을 확인하고, 기본 환경에서 UI와 문서가 노출되지 않는지도 확인한다. 독립 리뷰와 필요한 브라우저 QA 후 지적 사항만 재확인한다. 문서 동기화는 실제 결과에 맞춰 별도 배정한다.
6. 커밋·push·PR은 이번 사용자 지시에 포함되지 않는다.

## 완료 기준과 검증

- 로컬 UI에서 다섯 경로와 필수 입력, 성공 응답과 주요 오류가 실제 코드와 일치하며 요청을 실행할 수 있다. 누락·빈 입력과 실패 응답도 확인한다.
- 기존 API 동작·공통 응답을 보존하고 기본/배포 설정에서 UI와 OpenAPI JSON이 노출되지 않는다.
- 대상 서비스에서 Windows ` .\gradlew.bat test bootJar`, 필요 시 ` .\gradlew.bat bootRun --args='--spring.profiles.active=local'`을 실행한다. 실제 서버·브라우저 확인과 자동 테스트를 구분해 보고한다.
- 실제 요청은 격리 MySQL·Redis, 필요한 HMAC 설정과 테스트 메일 수신 및 Brevo 설정이 있어야 전체 흐름을 검증할 수 있다. 환경이 없거나 사용 권한이 없으면 실행하지 않고 미검증으로 기록한다. 공유 DB에 스키마를 임의 적용하지 않는다. 실제 메일 수신과 DB 저장 확인 여부도 구분한다.

## 승인과 진행 상태

- 사용자 지시에 따라 기존 #7 브랜치에서 구현과 문서 동기화가 진행 중이다. 검증·리뷰·QA 결과는 아직 확정되지 않았다.
- 메인이 검증 결과와 기존 명세 불일치의 처리 여부를 판단한다.
- 중단 조건: 계획 또는 이슈 승인 대기, 필수 정책 충돌이 이번 범위에 영향을 주는 경우, 중요한 범위·설계 변경, 권한·환경 차단, 수정 확인 후 미해결 지적, 사용자 중단 요청.
