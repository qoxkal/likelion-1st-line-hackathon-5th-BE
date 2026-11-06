# Feature Coverage

| 제품 흐름 | 관련 API | 데이터 저장 | 자동 테스트 | 외부 연동 |
|---|---|---:|---:|---:|
| 앱 시작/게스트 로그인 | `/auth/guest`, `/auth/refresh` | O | O | 불필요 |
| 기본 정보 입력 | `/me`, `/children` | O | 일부 | 불필요 |
| 책 탐색 | `/books` | O | Context | 불필요 |
| 페이지 촬영 | `/uploads/image`, `/books/pages` | O | 미작성 | Storage 선택 |
| 표현 분석 | `/analysis-jobs`, `/books/pages/{id}/analysis` | O | Context | AI 필요 |
| 단어 탐색 | `/vocabulary/{id}` | O | Context | 불필요 |
| 복습 보관함 | `/bookmarks` | O | 미작성 | 불필요 |
| 문장 연습 | `/sentence-practices` | O | 미작성 | AI 권장 |
| 대화 연습 | `/conversation-sessions` | O | 미작성 | AI/TTS 권장 |
| 오늘의 복습 | `/reviews/today`, `/answers` | O | 미작성 | 불필요 |
| 학습 현황 | `/learning-summary` | O | 미작성 | 불필요 |
| 알림 | `/notification-settings`, `/devices` | O | Context | FCM/APNs 필요 |
| 근거 기반 AI 답변 | `/ai/answers` | O | Python core | 실제 LLM 선택 |

## 커버리지 해석

- `O`: 현재 코드에 구현됨
- `일부`: 핵심 happy path만 검증됨
- `Context`: Spring Context에서 빈·JPA query 생성만 검증됨
- `미작성`: API 구현은 있으나 전용 시나리오 테스트가 필요함
