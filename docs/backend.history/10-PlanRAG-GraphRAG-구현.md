# 10. PlanRAG·GraphRAG 구현

## 1. 구성

```text
Spring Backend
  ├─ 사용자 인증·아이 소유권 검증
  ├─ 검수 표현·페이지·학습 이력 수집
  ├─ 신뢰 가능한 그래프 생성
  └─ POST ai-service/v1/answers

AI Service
  ├─ QueryRouter
  ├─ BoundedPlanner
  ├─ HybridRetriever
  ├─ GraphIndex Local/Global Search
  ├─ AnswerGenerator
  └─ EvidenceVerifier
```

## 2. 라우팅

| Route | 사용 조건 | 검색 방식 |
|---|---|---|
| `HYBRID` | 단순 정의·예문 | BM25 + semantic feature hashing |
| `GRAPH_LOCAL` | 유사 표현·관계·사용 상황 | 관련 node에서 depth 2 확장 |
| `GRAPH_GLOBAL` | 전체 주제·경향·요약 | connected component community 요약 |
| `PLAN` | 연령·학습 이력·표현을 함께 고려 | profile/history/hybrid/graph 도구 계획 |

Plan은 허용된 도구만 사용할 수 있고 기본 최대 4단계다. 무제한 agent loop는 허용하지 않는다.

## 3. 그래프 신뢰성

그래프의 node와 edge는 AI가 자유롭게 추출하지 않는다. Spring이 소유권 검증을 마친 DB 데이터에서 생성한다.

- Node: `CHILD_PROFILE`, `EXPRESSION`, `TYPE`, `BOOK_PAGE`
- Edge: `LEARNING_TARGET`, `HAS_TYPE`, `FROM_PAGE`, `SAME_TYPE`

이 방식은 잘못 생성된 관계가 장기 지식으로 저장되는 위험을 줄인다.

## 4. 정확성 보호

1. 원문 evidence ID를 모든 답변 항목에 표시한다.
2. 존재하지 않는 citation을 verifier가 거부한다.
3. 답변 token과 evidence token의 coverage를 검사한다.
4. retrieval confidence가 임계값보다 낮으면 답변하지 않는다.
5. verifier 실패 시 생성 답변을 사용자에게 노출하지 않는다.
6. Spring에서 child·expression 소유권을 먼저 검사한다.

## 5. 실행

개별 실행:

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e ".[dev]"
uvicorn booktalk_ai.api:app --port 8090
```

전체 실행:

```powershell
docker compose up --build
```

## 6. 실제 LLM 교체 지점

현재 `ExtractiveAnswerGenerator`는 evidence의 문장을 인용하는 결정적 구현이다. 실제 모델을 연결할 때
동일한 입력 evidence와 citation 형식을 강제하는 새 generator를 구현한다. 검색·계획·검증 계층은 유지한다.

실제 모델이 반환한 문장도 `EvidenceVerifier`를 통과하지 못하면 답변을 보류해야 한다.
