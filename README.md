# louie-kury-auth-service

루이쿠리 인증 서비스입니다. 로컬 실행에는 JDK 17이 필요하며 Gradle Wrapper를 사용합니다.

## 외부 MySQL 연결과 실행

저장소 루트에 `.env`를 준비합니다. 처음에는 `.env.example`을 복사하고 `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`를 실제 데이터베이스 접속 정보로 설정합니다. 기존 `.env`가 있으면 필요한 항목만 확인합니다. 실제 비밀번호는 로컬에만 보관합니다.

Spring Boot는 저장소 루트의 `.env`를 Java properties 형식으로 읽습니다. 셸 환경변수는 파일 값보다 우선합니다. 앱은 Hibernate DDL 자동 생성을 비활성화합니다.

Windows PowerShell

```powershell
.\gradlew.bat bootRun
```

macOS·Linux

```sh
./gradlew bootRun
```

## 선택 사항: 로컬 MySQL 컨테이너

`compose.yaml`은 로컬에서 별도의 MySQL을 실행할 때 사용합니다. 외부 MySQL 연결에는 이 컨테이너가 필요하지 않습니다. 로컬 DB를 사용하려면 `.env`에서 `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`, `MYSQL_HOST_PORT`를 설정하고 다음 명령을 실행합니다.

```sh
docker compose config --quiet
docker compose up -d mysql
```

앱에서도 로컬 DB를 사용하려면 `MYSQL_URL`을 `jdbc:mysql://127.0.0.1:3307/louie_kury_auth_dev`로, `MYSQL_USERNAME`을 `auth_service`로 설정합니다. 포트를 바꿨다면 URL에도 같은 포트를 사용합니다. 초기화된 볼륨의 비밀번호는 `.env` 수정만으로 변경되지 않습니다.
## 빌드 및 테스트

Windows에서는 `.\gradlew.bat test bootJar`, macOS·Linux에서는 `./gradlew test bootJar`를 실행합니다. 실행 JAR는 `build/libs/`에 생성됩니다.

## 공통 응답과 감사 시각

성공 응답은 `{"data":...}`, 오류 응답은 `{"code":"...","message":"..."}` 형식입니다. HTTP 상태는 웹 계층에서 결정하며, 서버 오류에는 내부 상세를 포함하지 않습니다.

JPA `BaseEntity`는 생성·수정 시각과 최초 삭제 시각을 관리합니다. JPA는 한국 시각을 `LocalDateTime`으로 다룹니다. API에서 시각을 반환할 때는 `ApiTime`으로 `+09:00` 오프셋을 적용합니다. 로그 시간대는 `Asia/Seoul`입니다.
