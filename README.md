# louie-kury-auth-service

루이쿠리 인증 서비스입니다. 로컬 실행에는 JDK 17이 필요하며 Gradle Wrapper를 사용합니다.

## 외부 MySQL 연결과 실행

저장소 루트에 `.env` 파일을 만들고 실제 데이터베이스 접속 정보를 설정합니다. 기존 `.env`가 있으면 필요한 항목만 확인합니다. 실제 비밀번호는 로컬에만 보관합니다.

```properties
MYSQL_URL=jdbc:mysql://<호스트>:<포트>/<스키마>
MYSQL_USERNAME=<사용자명>
MYSQL_PASSWORD=<비밀번호>
```

Spring Boot는 저장소 루트의 `.env`를 Java properties 형식으로 읽습니다. 셸 환경변수는 파일 값보다 우선합니다. 앱은 Hibernate DDL 자동 생성을 비활성화합니다.

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
