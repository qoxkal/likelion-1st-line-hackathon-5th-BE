# 03. API 명세

Base URL: `/v1`

`POST /auth/guest`, `POST /auth/refresh`, `GET /health`를 제외한 API는 Bearer Token이 필요하다.

## 1. 인증

1. `POST /auth/guest` — 게스트 계정 및 토큰 쌍 발급
2. `POST /auth/refresh` — Refresh Token 회전
3. `POST /auth/logout` — 현재 토큰 또는 모든 기기 세션 폐기

## 2. 사용자와 아이

1. `GET /me` — 내 계정 조회
2. `PATCH /me` — 내 계정 수정
3. `GET /children` — 아이 목록
4. `POST /children` — 아이 생성
5. `PATCH /children/{childId}` — 아이 수정
6. `DELETE /children/{childId}` — 확인 문구를 사용한 아이 삭제

## 3. 도서와 업로드

1. `GET /books` — 도서 목록 및 cursor 검색
2. `GET /books/{bookId}` — 도서 상세
3. `POST /books/uploads` — multipart 업로드와 페이지 등록을 한 번에 수행
4. `POST /uploads/image` — 업로드 세션 생성
5. `PUT /uploads/{uploadId}/content` — 파일 본문 업로드
6. `POST /books/pages` — 완료된 업로드를 책 페이지로 등록

## 4. 페이지 분석

1. `POST /books/{bookId}/pages/analyze` — 기존 호환 분석 시작
2. `GET /analysis/{jobId}` — 기존 호환 상태 조회
3. `POST /books/pages/{pageId}/analysis-jobs` — 페이지 분석 시작
4. `GET /analysis-jobs/{jobId}` — 분석 상태 조회
5. `GET /books/pages/{pageId}/analysis` — OCR 및 표현 결과 조회
6. `POST /analysis-jobs/{jobId}/cancel` — 진행 작업 취소

## 5. 표현과 학습

1. `GET /vocabulary/{vocabularyId}` — 표현 상세
2. `POST /children/{childId}/bookmarks` — 북마크 저장
3. `DELETE /children/{childId}/bookmarks/{vocabularyId}` — 북마크 해제
4. `GET /children/{childId}/bookmarks` — 북마크 목록
5. `POST /children/{childId}/learning-events` — 멱등 학습 이벤트 저장
6. `POST /children/{childId}/sentence-practices` — 문장 첨삭 요청
7. `GET /children/{childId}/sentence-practices` — 문장 연습 기록

## 6. 대화

1. `POST /children/{childId}/conversation-sessions` — 대화 시작
2. `POST /conversation-sessions/{sessionId}/messages` — 메시지 전송
3. `POST /conversation-sessions/{sessionId}/complete` — 대화 종료
4. `GET /conversation-sessions/{sessionId}` — 대화 기록 조회

## 7. 복습과 요약

1. `GET /children/{childId}/reviews/today` — 오늘의 복습
2. `POST /reviews/{reviewId}/answers` — 복습 답안 제출
3. `GET /children/{childId}/learning-summary` — 주간·월간 학습 요약

## 8. 알림과 음성

1. `GET /notification-settings` — 알림 설정
2. `PATCH /notification-settings` — 알림 설정 변경
3. `POST /devices` — 푸시 기기 등록
4. `POST /speech/synthesis` — TTS 요청
5. `POST /speech/transcriptions` — STT 작업 요청

## 9. 응답 형식

성공:

```json
{"data": {}, "meta": {"requestId": "req_...", "timestamp": "..."}}
```

실패:

```json
{"error": {"code": "VALIDATION_ERROR", "message": "...", "details": [], "retryable": false}, "meta": {}}
```
