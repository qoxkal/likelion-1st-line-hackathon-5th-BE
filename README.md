# 아이랑 북토크 백엔드

`feat/backend-setup`의 Spring Boot 4 / Java 21 코드를 기준으로 제품 API를 구현한 백엔드입니다.
모든 URL은 `/v1`을 기준으로 하며, `/auth/guest`, `/auth/refresh`, `/health`를 제외하면
`Authorization: Bearer {accessToken}` 헤더가 필요합니다.

## 로컬 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

기본 `local` 프로필은 H2 메모리 DB를 사용합니다. 운영 환경에서는 아래 값을 주고 `prod` 프로필을 사용합니다.

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://host:5432/postgres
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=32-byte-or-longer-random-secret
STORAGE_DIR=/private/path/uploads
```

검증은 `.\gradlew.bat test`로 실행합니다.

PlanRAG/GraphRAG 답변을 사용하려면 저장소의 `ai-service`를 8090 포트로 먼저 실행하거나,
저장소 루트에서 `docker compose up --build`를 실행합니다.

## 구현 범위

- 게스트 로그인, refresh token 회전/재사용 감지, 로그아웃
- 내 계정 및 아이 프로필 CRUD
- 도서 목록/상세, 직접 multipart 업로드와 signed-URL 방식의 2단계 업로드
- 비동기 페이지 분석, 상태/결과 조회, 취소
- 표현 상세, 북마크, 학습 이벤트, 문장 첨삭
- AI 대화 세션/메시지/완료/조회
- 오늘의 복습, 답안 처리, 학습 요약
- 알림 설정 및 푸시 기기 등록
- TTS/STT 계약 API
- Hybrid RAG, GraphRAG Local/Global, 제한형 PlanRAG 기반 `POST /v1/ai/answers`

페이지 분석, 문장 첨삭, 대화, TTS/STT의 외부 공급자가 아직 정해지지 않은 부분은 로컬 결정적
구현으로 동작합니다. `PageAnalyzer`와 각 서비스 경계를 실제 공급자 어댑터로 교체하면 API 계약과
DB 흐름을 유지할 수 있습니다. 업로드 원본은 외부 공개 경로가 아닌 저장소에 두고 분석 성공 후 삭제합니다.

## 주요 API 예시

```bash
curl -X POST http://localhost:8080/v1/auth/guest \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"device-install-uuid","locale":"ko"}'
```

성공/실패 응답은 각각 `{ "data", "meta" }`, `{ "error", "meta" }` 형식을 사용하며,
모든 응답에 `X-Request-Id`가 포함됩니다.
