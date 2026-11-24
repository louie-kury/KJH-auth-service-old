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
