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

## 빌드 및 테스트

Windows에서는 `.\gradlew.bat test bootJar`, macOS·Linux에서는 `./gradlew test bootJar`를 실행합니다. 실행 JAR는 `build/libs/`에 생성됩니다.
