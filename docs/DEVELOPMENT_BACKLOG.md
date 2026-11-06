# Development Backlog

## P0 — 배포 전 필수

1. Flyway migration 작성 및 `ddl-auto=validate` 전환
2. PostgreSQL Testcontainers 통합 테스트
3. Object Storage 실제 signed URL 및 retention worker
4. AI 입력·출력 안전 필터
5. Rate Limit과 사용자별 일일 quota
6. 아이 삭제를 예약 삭제/복구 유예 방식으로 변경
7. Secrets Manager 또는 배포 플랫폼 secret 연결
8. ExtractiveAnswerGenerator를 선택한 LLM provider 어댑터로 교체하고 verifier 회귀 테스트

## P1 — 제품 완성

1. 실제 페이지 분석 AI 어댑터
2. 실제 문장 첨삭과 대화 Agent
3. 실제 TTS/STT 공급자
4. FCM/APNs 알림 발송 worker
5. 공용 Vocabulary 마스터와 콘텐츠 검수 상태
6. 업로드·분석·학습 전체 E2E 테스트
7. 대화·복습 전체 E2E 테스트

## P2 — 운영과 확장

1. 운영자 인증, MFA, 역할 기반 권한
2. 분석 작업 검색·재시도 API
3. 콘텐츠 검수 API
4. 감사 로그
5. metrics, tracing, alerting
6. 큐 기반 분석 worker 분리
7. 캐시 및 CDN 전략

## 완료 정의

- 코드 구현
- 소유권 검증
- 오류 응답 정의
- 단위 또는 통합 테스트
- 문서 갱신
- 운영 환경변수와 관측성 정의
