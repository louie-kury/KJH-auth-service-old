# louie-kury-auth-service

루이쿠리 인증 서비스입니다. 로컬 실행에는 JDK 17이 필요하며 Gradle Wrapper를 사용합니다.

## 외부 MySQL 연결과 실행

저장소 루트에 `.env` 파일을 만들고 실제 데이터베이스 접속 정보를 설정합니다. 기존 `.env`가 있으면 필요한 항목만 확인합니다. 실제 비밀번호는 로컬에만 보관합니다.

```properties
MYSQL_URL=jdbc:mysql://<호스트>:<포트>/<스키마>
MYSQL_USERNAME=<사용자명>
MYSQL_PASSWORD=<비밀번호>
```

Spring Boot는 저장소 루트의 `.env`를 Java properties 형식으로 읽습니다. 예를 들어 실제 비밀번호가 `sample\value`이면 역슬래시를 두 번 써서 다음처럼 입력합니다.

```properties
MYSQL_PASSWORD=sample\\value
```

Java properties에서 역슬래시는 이스케이프 문자입니다. 값 앞의 공백은 `\ `로 입력하며, 따옴표는 값을 감싸는 구분자가 아니라 값 자체로 읽힙니다. 실제 비밀번호를 예제 파일이나 저장소에 기록하지 않습니다.

셸 환경변수를 직접 지정할 수도 있습니다. 셸 환경변수는 `.env`의 값보다 우선합니다. 아래 값은 예시이므로 실제 접속 정보로 바꿉니다.

Windows PowerShell

```powershell
$env:MYSQL_URL = 'jdbc:mysql://<호스트>:<포트>/<스키마>'
$env:MYSQL_USERNAME = '<사용자명>'
$env:MYSQL_PASSWORD = '<비밀번호>'
```

macOS·Linux

```sh
export MYSQL_URL='jdbc:mysql://<호스트>:<포트>/<스키마>'
export MYSQL_USERNAME='<사용자명>'
export MYSQL_PASSWORD='<비밀번호>'
```

셸에서 비밀번호에 특수문자나 따옴표가 포함되면 해당 셸의 인용 규칙을 따라 원래 값이 전달되도록 합니다. 앱은 Hibernate DDL 자동 생성을 비활성화합니다.

Windows PowerShell

```powershell
.\gradlew.bat bootRun
```

macOS·Linux

```sh
./gradlew bootRun
```

## 빌드 및 테스트

Windows에서는 `.\gradlew.bat test bootJar`, macOS·Linux에서는 `./gradlew test bootJar`를 실행합니다. 실행 JAR는 `build/libs/`에 생성됩니다.

## 공통 응답과 감사 시각

성공 응답은 `{"data":...}`, 오류 응답은 `{"code":"...","message":"..."}` 형식입니다. HTTP 상태는 웹 계층에서 결정하며, 서버 오류에는 내부 상세를 포함하지 않습니다.

JPA `BaseEntity`는 생성·수정 시각과 최초 삭제 시각을 관리합니다. JPA는 한국 시각을 `LocalDateTime`으로 다룹니다. API에서 시각을 반환할 때는 `ApiTime`으로 `+09:00` 오프셋을 적용합니다. 로그 시간대는 `Asia/Seoul`입니다.
# 회원가입 인증번호 발송 설정

`POST /api/auth/email-verifications`는 `email`, `purpose: SIGNUP`을 받아 인증번호를 발송합니다. 앞뒤 공백을 제거하고 ASCII 이메일만 허용하며 전체 대소문자를 구분하지 않습니다. Redis와 Brevo 트랜잭션 이메일 API가 필요합니다.

- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`: Redis 연결 설정입니다. 기본 호스트와 포트는 localhost와 6379입니다.
- `VERIFICATION_EMAIL_HMAC_KEY`, `VERIFICATION_CODE_HMAC_KEY`: 서로 다른 32바이트 이상의 비밀값입니다. 저장소에 넣지 마세요.
- `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`: Brevo API 키와 인증된 발신 주소입니다.
- `VERIFICATION_IP_LIMIT`, `VERIFICATION_IP_WINDOW_SECONDS`: IP 발송 제한입니다. 기본값은 한 시간에 100회입니다.

클라이언트 IP는 서블릿 연결의 원격 주소를 사용하며 임의의 전달 헤더는 신뢰하지 않습니다. 프록시 배포에서는 신뢰할 수 있는 프록시 설정이 없다면 모든 사용자가 같은 IP로 집계될 수 있습니다. 발송 실패 또는 결과가 불명확한 경우에도 제한 횟수는 차감되며 이전 인증번호는 무효로 유지됩니다. Brevo가 요청을 수락해도 실제 수신을 보장하지 않습니다.

## 이메일 인증번호 확인

발송 요청의 `verificationId`와 이메일로 받은 6자리 번호를 사용합니다. 인증번호, 증명, 비밀키는 저장소나 공유 로그에 기록하지 않습니다.

```http
POST /api/auth/email-verifications/{verificationId}/confirm
Content-Type: application/json

{"code":"123456"}
```

성공 시 `data.emailVerificationToken`과 `data.expiresAt`을 반환합니다. 증명은 해당 이메일과 가입 목적에 연결되며 10분간 유효합니다. 일반 가입 API에서 이 증명을 확인합니다. 성공한 번호는 재사용할 수 없습니다. 한 번호를 5회 틀리면 무효화하며, 이메일별 최근 1시간에 10회 실패하면 그 시점부터 1시간 동안 확인이 제한됩니다. 새 번호를 발송해도 실패 횟수나 확인 제한은 초기화되지 않습니다. 제한 중에도 발송 요청은 가능합니다.

만료된 인증 ID는 발송 시점부터 최대 24시간 동안 `VERIFICATION_EXPIRED`로 처리하며, 이후 또는 기존 표식이 없는 만료 ID는 `NOT_FOUND`로 처리합니다. 이미 확인되었거나 재발송·실패로 무효화된 ID는 `NOT_FOUND`입니다.

## 아이디·닉네임 중복 확인

가입 전에 아래 API로 사용 가능 여부를 확인할 수 있습니다.

```http
GET /api/auth/login-ids/availability?loginId=example
GET /api/auth/nicknames/availability?nickname=example
```

두 API 모두 사용 가능하면 `{"data":{"available":true}}`, 이미 사용 중이면 `{"data":{"available":false}}`를 반환합니다. 값이 없거나 공백뿐이거나, 로그인 아이디가 100자 또는 닉네임이 30자를 초과하면 `400 VALIDATION_ERROR`를 반환합니다. 길이는 Unicode 코드 포인트 기준입니다. 입력은 임의로 변환하지 않으며 비교는 MySQL 컬럼의 컬레이션을 따릅니다. 현재 컬레이션은 대소문자와 악센트를 구분하지 않습니다.

확인 결과는 해당 값을 예약하지 않습니다. 확인 후 다른 사용자가 먼저 사용할 수 있으므로 가입 시에도 DB의 유일 제약을 확인해야 합니다.

## 일반 회원가입

`POST /api/auth/signup`은 가입용 이메일 인증 증명과 가입 정보를 받아 MySQL의 계정, 로그인 정보, 프로필을 저장합니다. 성공하면 `201 Created`, `Location: /api/members/me`와 `{"data":{"memberId":"<생성된 UUID>"}}`를 반환합니다. 로그인은 별도 API에서 수행합니다.

가입 전에 이메일 인증번호 발송·확인을 완료하고 `emailVerificationToken`을 준비합니다. Redis 연결 및 인증번호 API에 필요한 이메일·코드 HMAC 키 외에 다음 설정과 MySQL 스키마가 필요합니다.

- `SIGNUP_IDEMPOTENCY_HMAC_KEY`: UTF-8 기준 32바이트 이상이며 `VERIFICATION_EMAIL_HMAC_KEY`, `VERIFICATION_CODE_HMAC_KEY`와 각각 다른 비밀값입니다. 로컬 환경변수 또는 `.env`에 설정하고 저장소나 로그에 남기지 않습니다.
- MySQL 대상 스키마에 기존 ERD의 `account`, `login_credential`, `user_profile` 테이블과 `src/main/resources/db/manual/001-signup-idempotency.sql`의 테이블이 필요합니다. 이 스크립트는 자동 실행되지 않으며 대상 스키마·권한을 확인한 뒤 명시적으로 적용해야 합니다. 실제 공유 DB에는 검토 없이 적용하지 마세요.

```http
POST /api/auth/signup
Content-Type: application/json
Idempotency-Key: <요청별 충분히 무작위인 고유 키>

{
  "name": "김보호",
  "phoneNumber": "01000000000",
  "gender": "FEMALE",
  "birthDate": "1995-01-01",
  "address": "서울특별시",
  "nickname": "루이집사",
  "loginId": "loui-care",
  "password": "<15~128자 비밀번호>",
  "email": "caregiver@example.com",
  "emailVerificationToken": "<가입용 이메일 인증 증명>"
}
```

- 모든 필드는 필수입니다. 성별은 `MALE` 또는 `FEMALE`, 전화번호는 숫자만 받습니다. 비밀번호는 Unicode 코드 포인트 15~128자이며 특정 문자 종류를 강제하지 않습니다. 닉네임은 최대 30자, 로그인 아이디는 최대 100자입니다.
- 신규 계정 상태는 `ACTIVE`, 프로필 유형은 `USER`입니다. 실제 약관이 확정되기 전까지 약관 동의 상태를 임시로 설정하며, 이는 실제 약관을 보여주고 동의를 받은 증거가 아닙니다.
- 인증 증명은 이메일과 `SIGNUP` 목적 및 만료를 Redis에서 확인합니다. 저장 시 증명 확인값·요청 확인값과 계정·로그인 정보·프로필을 MySQL 한 트랜잭션에 기록하며, 다른 가입에서 같은 증명을 재사용할 수 없습니다.
- 요청마다 새로운 무작위 `Idempotency-Key`를 만드세요. 가입 성공 후 최소 24시간 동안 같은 키와 완전히 같은 입력·증명으로 재시도하면 동일한 결과를 반환하며, 같은 키에 다른 입력을 보내면 `409 IDEMPOTENCY_CONFLICT`입니다. 저장 전 Redis 증명이 만료되거나 유실되면 다시 인증해야 합니다.
- 가입 완료 기록의 만료 시각은 저장 완료 처리 시점 기준 25시간입니다. 앱 실행 중 기본적으로 시작 60초 후부터 15분 간격으로 만료된 기록을 최대 500건씩 정리합니다. 정지, 오류, 대기 건수에 따라 실제 삭제는 늦어질 수 있으며 실패 시 다음 주기에 재시도합니다. 만료 이후에는 기존 결과 반환을 보장하지 않습니다.
- 입력 오류와 무효·만료·재사용 증명은 각각 `400 VALIDATION_ERROR`, `400 INVALID_EMAIL_VERIFICATION`입니다. 가입 정보 중복은 이메일 `409 EMAIL_ALREADY_REGISTERED`, 아이디 `409 LOGIN_ID_ALREADY_EXISTS`, 닉네임 `409 NICKNAME_ALREADY_EXISTS`로 구분합니다. 여러 값이 중복되면 이메일 → 아이디 → 닉네임 순으로 하나를 반환합니다. Redis 확인 장애는 `503 VERIFICATION_UNAVAILABLE`, DB 처리나 결과 확인에 실패하면 `503 SIGNUP_PENDING`입니다.
