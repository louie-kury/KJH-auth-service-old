# louie-kury-auth-service

루이쿠리 인증 서비스입니다. 로컬 실행에는 JDK 17과 Docker Compose가 필요합니다. 빌드에는 저장소의 Gradle Wrapper를 사용합니다.

## 로컬 MySQL 준비

Docker Desktop을 시작하고, 저장소 루트에서 `.env.example`을 `.env`로 복사합니다. 기존 `.env`가 있다면 필요한 항목만 보완합니다. `MYSQL_ROOT_PASSWORD`와 `MYSQL_PASSWORD`를 각각 로컬 개발용 비밀번호로 설정한 뒤 다음 명령으로 MySQL 8.4.13을 시작합니다.

```sh
docker compose config --quiet
docker compose up -d mysql
docker compose ps mysql
docker compose logs --tail=50 mysql
```

MySQL의 기본 접속 주소는 `127.0.0.1:3307`, 개발 DB는 `louie_kury_auth_dev`, 애플리케이션 사용자는 `auth_service`입니다. 3307이 이미 사용 중이면 `.env`의 `MYSQL_HOST_PORT`와 아래 `MYSQL_URL`의 포트를 함께 바꿉니다. 데이터는 `louie-kury-auth-mysql-data` 볼륨에 저장됩니다. 초기화된 볼륨의 사용자 비밀번호는 `.env`를 수정하는 것만으로 변경되지 않습니다.

앱을 실행하기 전에 서비스 계정으로 개발 DB에 접속해 `SELECT 1`을 실행합니다. 비밀번호 프롬프트에는 `MYSQL_PASSWORD`로 설정한 값을 입력합니다. 결과에 `1`이 표시되면 앱 실행 단계로 넘어갑니다. 접속이 실패하면 MySQL 초기화 로그와 서비스 계정 설정을 확인한 뒤 다시 실행합니다.

```sh
docker compose exec mysql mysql --protocol=TCP -h 127.0.0.1 -u auth_service -p -D louie_kury_auth_dev -e "SELECT 1"
```

## 애플리케이션 연결과 실행

Compose는 `.env`를 읽습니다. 호스트의 Spring Boot에는 아래 값을 현재 셸이나 IDE의 실행 환경변수로 별도로 지정합니다.

| 환경변수 | 설정 |
| --- | --- |
| `MYSQL_URL` | 로컬 기본값 `jdbc:mysql://127.0.0.1:3307/louie_kury_auth_dev` |
| `MYSQL_USERNAME` | 로컬 기본값 `auth_service` |
| `MYSQL_PASSWORD` | 로컬 MySQL을 시작할 때 지정한 서비스 사용자 비밀번호 |

실제 접속 정보는 로컬 환경에만 보관합니다. 앱은 Hibernate DDL 자동 생성을 비활성화합니다.

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
