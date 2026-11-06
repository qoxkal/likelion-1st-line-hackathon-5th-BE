# 백엔드 문서

이 폴더는 `feat/backend-setup`에 있던 기존 구현과 `codex/backend-completion`에서 추가한 구현을 함께 설명한다.

## 문서 구성

1. [`backend.history/01-작업-이력.md`](backend.history/01-작업-이력.md) — 기존 작업과 추가 작업의 변경 이력
2. [`backend.history/02-구조와-핵심-개념.md`](backend.history/02-구조와-핵심-개념.md) — 패키지 구조와 공통 설계
3. [`backend.history/03-API-명세.md`](backend.history/03-API-명세.md) — 현재 구현된 API 목록
4. [`backend.history/04-의사결정-기록.md`](backend.history/04-의사결정-기록.md) — 주요 기술 선택과 이유
5. [`backend.history/05-DB-스키마.md`](backend.history/05-DB-스키마.md) — 엔티티 및 관계
6. [`backend.history/06-개발-가이드.md`](backend.history/06-개발-가이드.md) — 실행, 테스트, 배포 설정
7. [`backend.history/07-현황과-남은-과제.md`](backend.history/07-현황과-남은-과제.md) — 완료 범위와 후속 작업
8. [`backend.history/08-AI-Agent-연동-가이드.md`](backend.history/08-AI-Agent-연동-가이드.md) — AI/TTS/STT 공급자 교체 지점
9. [`backend.history/09-남은-검증-과제와-체크리스트.md`](backend.history/09-남은-검증-과제와-체크리스트.md) — 출시 전 검증 항목

상위 요약 문서:

- [`API_FEATURE_SUMMARY.md`](API_FEATURE_SUMMARY.md)
- [`FEATURE_COVERAGE.md`](FEATURE_COVERAGE.md)
- [`DEVELOPMENT_BACKLOG.md`](DEVELOPMENT_BACKLOG.md)

기능 상태 표기:

- **기존**: `feat/backend-setup`에 이미 있던 코드
- **추가**: 이번 백엔드 확장 작업에서 구현한 코드
- **외부 연동 필요**: API와 내부 흐름은 있으나 실제 공급자 연결이 필요한 기능
