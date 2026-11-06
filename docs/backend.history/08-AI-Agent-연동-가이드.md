# 08. AI Agent 연동 가이드

## 1. 페이지 분석

교체 지점: `domain.analysis.ai.PageAnalyzer`

실제 구현은 다음 결과를 반환해야 한다.

- OCR 원문
- 표현 텍스트와 유형
- 쉬운 한국어 정의
- 아이 프로필 언어 번역
- 원문 예문
- confidence

현재 `FakePageAnalyzer`가 로컬 동작을 담당한다. 실제 어댑터는 API 키를 환경변수로 받고, 이미지나
아이 이름을 로그에 남기지 않아야 한다.

## 2. 문장 첨삭

교체 지점: `LearningService.practice`

권장 추출 인터페이스:

```java
SentenceFeedback assess(String targetExpression, String sentence, String context, int childAge);
```

응답은 assessment, 의미 전달 여부, 목표 표현 사용 여부, 제안 문장, 쉬운 설명, highlight를 제공해야 한다.

## 3. 대화 Agent

교체 지점: `ConversationService.message`

필수 정책:

1. 세션당 최대 turn 제한
2. 목표 표현 사용 여부 구조화 출력
3. 아이 연령에 맞는 짧은 답변
4. 유해 콘텐츠 입력·출력 필터
5. 요청·응답 timeout과 재시도
6. 프롬프트 버전 기록

## 4. TTS/STT

- TTS: 텍스트+언어+voice+speed를 캐시 키로 사용한다.
- STT: 원본 음성은 비공개 저장하고 retention 이후 삭제한다.
- 공급자 실패는 `AI_PROVIDER_UNAVAILABLE` 또는 전용 오류 코드로 매핑한다.

## 5. 운영 관측성

- API requestId를 AI 공급자 요청 메타데이터와 연결한다.
- 모델명, 프롬프트 버전, 처리 시간, 토큰 사용량만 기록한다.
- 원본 이미지, 음성, 아이 별칭, 토큰은 로그에서 제외한다.
