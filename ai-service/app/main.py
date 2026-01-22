import json
import os
from typing import Any, Dict, List, Optional

import psycopg
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from langchain_upstage import ChatUpstage, UpstageEmbeddings
from langchain_postgres import PGVector


app = FastAPI(title="approval-ops-agent ai-service")


# ---- env ----
PG_CONN = os.getenv("PG_CONN", "postgresql+psycopg://app:app@db:5432/approvalops")
VECTOR_COLLECTION = os.getenv("VECTOR_COLLECTION", "ops_manual")

# Upstage
# langchain-upstage는 UPSTAGE_API_KEY를 env에서 읽음
UPSTAGE_API_KEY = os.getenv("UPSTAGE_API_KEY")
LLM_MODEL = os.getenv("LLM_MODEL", "solar-mini")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "solar-embedding-1-large")

if not UPSTAGE_API_KEY:
    raise RuntimeError("UPSTAGE_API_KEY is required")


def _raw_pg_conn_for_psycopg(conn: str) -> str:
    return conn.replace("postgresql+psycopg://", "postgresql://")


def ensure_pgvector_extension():
    raw = _raw_pg_conn_for_psycopg(PG_CONN)
    with psycopg.connect(raw) as conn:
        with conn.cursor() as cur:
            cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
        conn.commit()


def llm_client() -> ChatUpstage:
    # ChatUpstage는 env UPSTAGE_API_KEY를 읽고, model 파라미터를 받음
    return ChatUpstage(api_key=UPSTAGE_API_KEY, model=LLM_MODEL, temperature=0.2)


def vector_store() -> PGVector:
    # UpstageEmbeddings는 model="solar-embedding-1-large" 같은 이름을 사용
    embeddings = UpstageEmbeddings(model=EMBEDDING_MODEL)
    # PGVector는 psycopg3 드라이버를 위해 postgresql+psycopg:// 형식을 사용
    return PGVector(
        embeddings=embeddings,
        collection_name=VECTOR_COLLECTION,
        connection=PG_CONN,
        use_jsonb=True,
    )


@app.on_event("startup")
def startup():
    ensure_pgvector_extension()


# ---- schemas ----
class ToolSpec(BaseModel):
    name: str
    description: str
    args_schema: Dict[str, Any]


class PlanRequest(BaseModel):
    request_id: int
    requester: str
    title: str
    inputText: str
    tools: List[ToolSpec]
    top_k: int = 4


class RagIngestItem(BaseModel):
    doc_id: str
    text: str
    meta: Optional[Dict[str, Any]] = None


class RagAnswerRequest(BaseModel):
    question: str
    top_k: int = 4


@app.get("/health")
def health():
    return {"ok": True, "collection": VECTOR_COLLECTION}


@app.post("/rag/ingest")
def rag_ingest(items: List[RagIngestItem]) -> Dict[str, Any]:
    """
    벡터DB에 문서 적재(데모/초기 데이터)
    """
    vs = vector_store()
    texts = [i.text for i in items]
    metadatas = [({"doc_id": i.doc_id, **(i.meta or {})}) for i in items]
    ids = [i.doc_id for i in items]
    vs.add_texts(texts=texts, metadatas=metadatas, ids=ids)
    return {"ingested": len(items), "collection": VECTOR_COLLECTION}


@app.post("/rag/answer")
def rag_answer(req: RagAnswerRequest) -> Dict[str, Any]:
    """
    QnA(옵션 데모)
    """
    vs = vector_store()
    docs = vs.similarity_search(req.question, k=req.top_k)

    context = "\n\n".join([f"[{i}] {d.page_content}" for i, d in enumerate(docs)])
    sources = [d.metadata for d in docs]

    system = (
        "너는 사내 업무 도우미야. 주어진 근거(context) 범위 안에서만 답해.\n"
        "근거가 부족하면 '근거 부족'이라고 말하고, 어떤 자료가 더 필요한지 말해.\n"
    )
    user = f"질문: {req.question}\n\n근거(context):\n{context}"

    llm = llm_client()
    answer = llm.invoke([("system", system), ("human", user)]).content
    return {"answer": answer, "sources": sources}


@app.post("/plan")
def make_plan(req: PlanRequest) -> Dict[str, Any]:
    """
    Agent Planner
    RAG 검색 + LLM -> Plan JSON 생성
    """
    # 1) 근거 문서 검색
    vs = vector_store()
    docs = vs.similarity_search(req.inputText, k=req.top_k)
    context = "\n\n".join([f"[{i}] {d.page_content}" for i, d in enumerate(docs)])
    sources = [d.metadata for d in docs]

    # 2) tool 목록(스키마 포함)을 LLM에게 제공
    tools_text = "\n".join(
        [
            f"- {t.name}: {t.description} args={json.dumps(t.args_schema, ensure_ascii=False)}"
            for t in req.tools
        ]
    )

    system = (
        "너는 '업무 실행 계획(Plan) 생성기'야.\n"
        "사용자 요청을 아래 도구(tool) 호출 단계로 분해해서 JSON 계획만 출력해.\n"
        "규칙:\n"
        "1) 반드시 JSON만 출력 (코드블록 금지)\n"
        "2) steps[*].tool은 제공된 도구 이름 중 하나\n"
        "3) steps[*].args는 args_schema에 맞게 작성\n"
        "4) 안전/권한/외부시스템 변경 등 위험 소지가 있으면 approval_required=true\n"
        "5) 근거(context)가 있으면 그 근거를 기반으로 계획을 세워\n"
        "6) ticket.createMany 도구를 사용할 때는 count/titlePrefix 방식 대신 items 배열로 정확히 3개를 만들고,"
        "   각 items의 title/desc가 '원인분석/조치/검증'으로 서로 다르게 드러나게 작성해.\n"
    )

    user = (
        f"[요청ID] {req.request_id}\n"
        f"[요청자] {req.requester}\n"
        f"[제목] {req.title}\n"
        f"[요청내용] {req.inputText}\n\n"
        f"[근거(context)]\n{context}\n\n"
        f"[사용가능 도구]\n{tools_text}\n\n"
        "출력 JSON 스키마:\n"
        "{\n"
        '  "version": "1",\n'
        '  "approval_required": true|false,\n'
        '  "steps": [\n'
        '    {"tool": "...", "args": {...}, "why": "한 줄 근거"}\n'
        "  ]\n"
        "}\n"
    )

    llm = llm_client()
    out = llm.invoke([("system", system), ("human", user)]).content

    try:
        plan = json.loads(out)
    except Exception:
        raise HTTPException(
            status_code=502,
            detail={"reason": "LLM output is not valid JSON", "raw": out},
        )

    return {"plan": plan, "sources": sources}
