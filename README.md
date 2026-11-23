# louie-kury-auth-service

루이쿠리 인증 서비스입니다. 로컬 실행에는 JDK 17, Docker Compose, MongoDB Atlas 개발 환경이 필요합니다. 빌드에는 저장소의 Gradle Wrapper를 사용합니다.

## 로컬 MySQL 준비

Docker Desktop을 시작하고, 저장소 루트에서 `.env.example`을 `.env`로 복사합니다. 기존 `.env`가 있다면 필요한 항목만 보완합니다. `MYSQL_ROOT_PASSWORD`와 `MYSQL_PASSWORD`를 각각 로컬 개발용 비밀번호로 설정한 뒤 다음 명령으로 MySQL 8.4.13을 시작합니다.

```sh
docker compose config --quiet
docker compose up -d mysql
docker compose ps mysql
docker compose logs --tail=50 mysql
```

MySQL의 기본 접속 주소는 `127.0.0.1:3307`, 개발 DB는 `louie_kury_auth_dev`, 애플리케이션 사용자는 `auth_service`입니다. 3307이 이미 사용 중이면 `.env`의 `MYSQL_HOST_PORT`와 아래 `MYSQL_URL`의 포트를 함께 바꿉니다. 데이터는 `louie-kury-auth-mysql-data` 볼륨에 저장됩니다. 초기화된 볼륨의 사용자 비밀번호는 `.env`를 수정하는 것만으로 변경되지 않습니다.

## 애플리케이션 연결과 실행

Compose는 `.env`를 읽습니다. 호스트의 Spring Boot에는 아래 값을 현재 셸이나 IDE의 실행 환경변수로 별도로 지정합니다.

| 환경변수 | 설정 |
| --- | --- |
| `MYSQL_URL` | 로컬 기본값 `jdbc:mysql://127.0.0.1:3307/louie_kury_auth_dev` |
| `MYSQL_USERNAME` | 로컬 기본값 `auth_service` |
| `MYSQL_PASSWORD` | 로컬 MySQL을 시작할 때 지정한 서비스 사용자 비밀번호 |
| `MONGODB_URI` | Atlas 개발 환경의 연결 URI |
| `MONGODB_DATABASE` | 선택 항목. 기본값 `louie_kury_auth_dev` |

Atlas DB 사용자의 접근 권한과 네트워크 접근 설정을 확인합니다. 연결 URI에 들어가는 사용자명이나 비밀번호의 예약 문자는 URI 규칙에 맞게 인코딩합니다. 실제 접속 정보는 로컬 환경에만 보관합니다. 앱은 Hibernate DDL과 MongoDB 인덱스 자동 생성을 비활성화합니다.

Windows PowerShell

```powershell
.\gradlew.bat bootRun
```

macOS·Linux

```sh
./gradlew bootRun
```

## MongoDB 마이그레이션

MongoDB 변경은 앱을 시작하기 전에 별도 Gradle 명령으로 실행합니다. 현재 변경 로그는 비어 있으며, 첫 실행 시 Liquibase의 이력·잠금 컬렉션을 준비합니다. 마이그레이션 도구의 의존성은 애플리케이션 실행 JAR와 분리되어 있습니다.

| 환경변수 | 설정 |
| --- | --- |
| `MONGODB_MIGRATION_URI` | 대상 MongoDB와 마이그레이션 권한이 있는 전용 계정의 연결 URI |
| `MONGODB_MIGRATION_DATABASE` | 대상 서비스 DB 이름. 로컬 개발용 Atlas 예시는 `louie_kury_auth_dev` |

두 값을 실행할 셸에 지정합니다. URI에 DB 이름이 있다면 환경변수의 이름과 같아야 합니다. 애플리케이션의 `MONGODB_URI`와 `MONGODB_DATABASE`는 마이그레이션 명령에 자동 전달되지 않습니다.

```powershell
.\gradlew.bat mongoMigrationStatus
.\gradlew.bat mongoMigrate
```

macOS·Linux에서는 `./gradlew mongoMigrationStatus`와 `./gradlew mongoMigrate`를 실행합니다. 상태 확인도 최초 실행 시 Liquibase 메타데이터를 초기화하므로 메타데이터 생성·조회 권한이 필요합니다. 이미 적용된 변경셋은 이력을 바탕으로 건너뜁니다. 다른 실행이 잠금을 보유한 경우 실패할 수 있으며, 실행 중인 작업을 확인한 뒤 다시 시도합니다.

## 빌드 및 테스트

Windows에서는 `.\gradlew.bat test bootJar`, macOS·Linux에서는 `./gradlew test bootJar`를 실행합니다. 실행 JAR는 `build/libs/`에 생성됩니다.

## 공통 응답과 감사 시각

성공 응답은 `{"data":...}`, 오류 응답은 `{"code":"...","message":"..."}` 형식입니다. HTTP 상태는 웹 계층에서 결정하며, 서버 오류에는 내부 상세를 포함하지 않습니다.

JPA `BaseEntity`와 MongoDB `BaseDocument`는 생성·수정 시각과 최초 삭제 시각을 관리합니다. JPA는 한국 시각을 `LocalDateTime`으로 다루고 MongoDB는 `Instant`를 사용합니다. API에서 시각을 반환할 때는 `ApiTime`으로 `+09:00` 오프셋을 적용합니다. 로그 시간대는 `Asia/Seoul`입니다.
