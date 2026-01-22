CREATE TABLE IF NOT EXISTS work_request (
    id           BIGSERIAL PRIMARY KEY,
    requester    TEXT NOT NULL,
    title        TEXT NOT NULL,
    input_text   TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'DRAFT',
    plan_json    TEXT,
    approved_by  TEXT,
    approved_at  TIMESTAMPTZ,
    executed_at  TIMESTAMPTZ,
    result_json  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit_log (
    id           BIGSERIAL PRIMARY KEY,
    request_id   BIGINT NOT NULL REFERENCES work_request(id) ON DELETE CASCADE,
    at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor        TEXT NOT NULL,
    action       TEXT NOT NULL,
    message      TEXT NOT NULL,
    success      BOOLEAN NOT NULL,
    latency_ms   INT NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket (
    id           BIGSERIAL PRIMARY KEY,
    request_id   BIGINT NOT NULL REFERENCES work_request(id) ON DELETE CASCADE,
    title        TEXT NOT NULL,
    description  TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS wiki_page (
    id           BIGSERIAL PRIMARY KEY,
    request_id   BIGINT NOT NULL REFERENCES work_request(id) ON DELETE CASCADE,
    title        TEXT NOT NULL,
    body         TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);