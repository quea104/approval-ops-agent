<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { apiFetch } from "../services/api.js";

const route = useRoute();
const id = computed(() => route.params.id);

const loading = ref(false);
const acting = ref(false);
const err = ref("");
const ok = ref("");

const data = ref(null);

const req = computed(() => data.value?.request || {});
const tickets = computed(() => data.value?.tickets || []);
const wikiPages = computed(() => data.value?.wikiPages || []);
const audit = computed(() => data.value?.audit || []);

function pretty(jsonText) {
  try {
    return JSON.stringify(JSON.parse(jsonText), null, 2);
  } catch {
    return jsonText;
  }
}

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text);
    ok.value = "복사 완료";
    setTimeout(() => (ok.value = ""), 1000);
  } catch {
    // 무시
  }
}

async function loadAll() {
  err.value = "";
  ok.value = "";
  loading.value = true;
  try {
    data.value = await apiFetch(`/api/requests/${id.value}`);
  } catch (e) {
    err.value = e?.message || String(e);
  } finally {
    loading.value = false;
  }
}

async function makePlan() {
  acting.value = true;
  err.value = "";
  ok.value = "";
  try {
    await apiFetch(`/api/requests/${id.value}/plan`, { method: "POST" });
    ok.value = "계획 생성 완료";
    await loadAll();
  } catch (e) {
    err.value = e?.message || String(e);
  } finally {
    acting.value = false;
  }
}

async function approve(decision) {
  acting.value = true;
  err.value = "";
  ok.value = "";
  try {
    await apiFetch(`/api/requests/${id.value}/approve`, {
      method: "POST",
      body: { decision, comment: decision === "APPROVE" ? "OK" : "반려" },
    });
    ok.value = decision === "APPROVE" ? "승인 완료" : "반려 완료";
    await loadAll();
  } catch (e) {
    err.value = e?.message || String(e);
  } finally {
    acting.value = false;
  }
}

async function execute() {
  acting.value = true;
  err.value = "";
  ok.value = "";
  try {
    await apiFetch(`/api/requests/${id.value}/execute`, { method: "POST" });
    ok.value = "실행 완료";
    await loadAll();
  } catch (e) {
    err.value = e?.message || String(e);
  } finally {
    acting.value = false;
  }
}

onMounted(loadAll);
</script>

<template>
  <div class="wrap">
    <div class="topline">
      <button class="btn" @click="$router.push('/requests')">← 목록</button>
      <div class="title">요청 상세 #{{ id }}</div>
      <button class="btn" @click="loadAll" :disabled="loading">{{ loading ? "불러오는 중..." : "새로고침" }}</button>
    </div>

    <p class="err" v-if="err">{{ err }}</p>

    <div v-if="data" class="grid">
      <!-- 왼쪽: 상태/액션 -->
      <section class="panel">
        <h2>요청</h2>
        <div class="kv">
          <div class="k">제목</div><div class="v">{{ req.title }}</div>
          <div class="k">상태</div><div class="v"><b>{{ req.status }}</b></div>
          <div class="k">요청자</div><div class="v">{{ req.requester }}</div>
          <div class="k">생성</div><div class="v">{{ req.created_at }}</div>
        </div>

        <h3>요청 내용</h3>
        <pre class="pre">{{ req.input_text }}</pre>

        <h3>작업</h3>
        <div class="actions">
          <button class="primary" @click="makePlan" :disabled="acting">계획 생성</button>
          <button class="btn" @click="approve('APPROVE')" :disabled="acting">승인</button>
          <button class="btn danger" @click="approve('REJECT')" :disabled="acting">반려</button>
          <button class="btn" @click="execute" :disabled="acting">실행</button>
        </div>

        <p class="ok" v-if="ok">{{ ok }}</p>
      </section>

      <!-- 오른쪽: 계획/결과/로그 -->
      <section class="panel">
        <h2>계획(Plan JSON)</h2>
        <div v-if="req.plan_json" class="box">
          <button class="btn small" @click="copy(req.plan_json)">복사</button>
          <pre class="pre">{{ pretty(req.plan_json) }}</pre>
        </div>
        <p v-else class="empty">아직 계획이 없습니다. “계획 생성” 버튼을 눌러주세요.</p>

        <hr class="hr" />

        <h2>결과물</h2>

        <h3>티켓</h3>
        <div v-if="tickets.length" class="list">
          <div class="item" v-for="t in tickets" :key="t.id">
            <div class="item-title">#{{ t.id }} {{ t.title }}</div>
            <div class="item-body">{{ t.description }}</div>
          </div>
        </div>
        <p v-else class="empty">생성된 티켓이 없습니다.</p>

        <h3>위키 문서</h3>
        <div v-if="wikiPages.length" class="list">
          <div class="item" v-for="w in wikiPages" :key="w.id">
            <div class="item-title">#{{ w.id }} {{ w.title }}</div>
            <div class="item-body">{{ w.body }}</div>
          </div>
        </div>
        <p v-else class="empty">생성된 위키 문서가 없습니다.</p>

        <hr class="hr" />

        <h2>감사 로그</h2>
        <div v-if="audit.length" class="audit">
          <div class="audit-row" v-for="a in audit" :key="a.id">
            <div class="a1">{{ a.at }}</div>
            <div class="a2"><b>{{ a.action }}</b></div>
            <div class="a3">{{ a.actor }}</div>
            <div class="a4" :class="{ bad: !a.success }">{{ a.success ? "성공" : "실패" }}</div>
            <div class="a5">{{ a.latency_ms }}ms</div>
            <div class="a6">{{ a.message }}</div>
          </div>
        </div>
        <p v-else class="empty">로그가 없습니다.</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.wrap { display:grid; gap:12px; }
.topline { display:flex; align-items:center; justify-content:space-between; gap:10px; }
.title { font-weight:900; }
.grid { display:grid; grid-template-columns: 0.9fr 1.3fr; gap:14px; align-items:start; }
@media (max-width: 980px) { .grid { grid-template-columns: 1fr; } }
.panel { border:1px solid #e6e6e6; border-radius:14px; padding:16px; }
h2 { margin:0 0 10px; }
h3 { margin:14px 0 8px; }
.kv { display:grid; grid-template-columns: 90px 1fr; gap:8px 10px; font-size:13px; }
.k { color:#666; }
.v { color:#111; }
.pre { white-space: pre-wrap; background:#0b0b0b; color:#ddd; padding:10px; border-radius:10px; font-size:12px; }
.actions { display:flex; flex-wrap:wrap; gap:8px; }
.btn { border:1px solid #ddd; background:#fff; padding:8px 10px; border-radius:10px; cursor:pointer; }
.btn:hover { background:#f7f7f7; }
.btn.small { padding:6px 8px; font-size:12px; }
.btn.danger { border-color:#f0caca; }
.primary { border:0; background:#111; color:#fff; padding:8px 12px; border-radius:10px; cursor:pointer; }
.primary:disabled, .btn:disabled { opacity:.6; cursor:not-allowed; }
.ok { color:#0a7a36; font-size:13px; margin-top:10px; }
.err { color:#b00020; font-size:13px; }
.hr { border:none; border-top:1px solid #eee; margin:16px 0; }
.empty { color:#666; font-size:13px; }
.list { display:grid; gap:10px; }
.item { border:1px solid #eee; border-radius:12px; padding:10px; }
.item-title { font-weight:800; margin-bottom:6px; }
.item-body { color:#333; font-size:13px; line-height:1.5; }
.box { position:relative; }
.audit { display:grid; gap:8px; }
.audit-row {
  border:1px solid #eee; border-radius:12px; padding:8px;
  display:grid; grid-template-columns: 150px 90px 90px 60px 70px 1fr;
  gap:8px; font-size:12px; color:#333;
}
@media (max-width: 980px) {
  .audit-row { grid-template-columns: 1fr; }
}
.bad { color:#b00020; font-weight:800; }
</style>