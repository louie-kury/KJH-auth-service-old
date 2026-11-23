# louie-kury-auth-service

루이쿠리의 인증 서비스입니다. 로컬 빌드와 실행에는 JDK 17이 필요합니다. 저장소에 포함된 Gradle Wrapper를 사용합니다.

## Windows PowerShell

```powershell
.\gradlew.bat bootRun
```

## macOS·Linux

```bash
sh ./gradlew bootRun
```

실행 JAR는 같은 Wrapper에서 `bootRun` 대신 `bootJar`를 실행하면 `build/libs/`에 생성됩니다.
