<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { apiFetch } from "../services/api.js";

const router = useRouter();

const title = ref("주간 안전점검 자동화");
const inputText = ref("이번 주 안전점검 티켓 5개 만들고, 점검 요약 위키 문서도 작성해줘.");
const creating = ref(false);
const ok = ref("");
const err = ref("");

const items = ref([]);
const loading = ref(false);
const listErr = ref("");

function goDetail(id) {
  router.push(`/requests/${id}`);
}

async function createRequest() {
  ok.value = "";
  err.value = "";
  creating.value = true;
  try {
    const r = await apiFetch("/api/requests", {
      method: "POST",
      body: { title: title.value, inputText: inputText.value },
    });
    ok.value = `요청 생성 완료! ID = ${r.id}`;
    await loadList();
    goDetail(r.id);
  } catch (e) {
    err.value = e?.message || String(e);
  } finally {
    creating.value = false;
  }
}

async function loadList() {
  listErr.value = "";
  loading.value = true;
  try {
    items.value = await apiFetch("/api/requests");
  } catch (e) {
    listErr.value = e?.message || String(e);
  } finally {
    loading.value = false;
  }
}

onMounted(loadList);
</script>

<template>
  <div class="grid">
    <section class="panel">
      <h2>요청 만들기</h2>

      <div class="row">
        <label>제목</label>
        <input v-model="title" placeholder="예: 주간 안전점검 자동화" />
      </div>

      <div class="row">
        <label>요청 내용(자연어)</label>
        <textarea v-model="inputText" rows="5"
                  placeholder="예: 이번 주 안전점검 티켓 5개 만들고, 점검 요약 위키 문서 작성해줘." />
      </div>

      <button class="primary" :disabled="creating || !title.trim() || !inputText.trim()" @click="createRequest">
        {{ creating ? "생성 중..." : "요청 생성" }}
      </button>

      <p class="ok" v-if="ok">{{ ok }}</p>
      <p class="err" v-if="err">{{ err }}</p>
    </section>

    <section class="panel">
      <div class="head">
        <h2>요청 목록</h2>
        <button class="btn" :disabled="loading" @click="loadList">
          {{ loading ? "불러오는 중..." : "새로고침" }}
        </button>
      </div>

      <p class="err" v-if="listErr">{{ listErr }}</p>

      <div v-if="items.length" class="list">
        <div class="item" v-for="it in items" :key="it.id" @click="goDetail(it.id)">
          <div class="title">
            #{{ it.id }} {{ it.title }}
          </div>
          <div class="meta">
            <span>상태: {{ it.status }}</span>
            <span>요청자: {{ it.requester }}</span>
            <span>생성: {{ it.created_at }}</span>
          </div>
        </div>
      </div>

      <p v-else class="empty">아직 요청이 없습니다.</p>
    </section>
  </div>
</template>

<style scoped>
.grid { display:grid; grid-template-columns: 1fr 1.2fr; gap:14px; }
@media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }
.panel { border:1px solid #e6e6e6; border-radius:14px; padding:16px; }
.head { display:flex; align-items:center; justify-content:space-between; gap:12px; }
h2 { margin:0 0 10px; }
.row { display:grid; gap:6px; margin:12px 0; }
label { font-size:13px; color:#444; }
input, textarea { padding:10px 12px; border:1px solid #ddd; border-radius:10px; outline:none; }
textarea { resize:vertical; }
.primary { width:100%; padding:10px 12px; border:0; border-radius:12px; cursor:pointer; background:#111; color:#fff; }
.primary:disabled { opacity:.6; cursor:not-allowed; }
.btn { border:1px solid #ddd; background:#fff; padding:7px 10px; border-radius:10px; cursor:pointer; }
.btn:hover { background:#f7f7f7; }
.list { display:grid; gap:10px; margin-top:10px; }
.item { border:1px solid #eee; border-radius:12px; padding:10px; cursor:pointer; }
.item:hover { background:#fafafa; }
.title { font-weight:800; }
.meta { display:flex; gap:10px; flex-wrap:wrap; font-size:12px; color:#666; margin-top:6px; }
.ok { color:#0a7a36; font-size:13px; margin-top:10px; }
.err { color:#b00020; font-size:13px; margin-top:10px; }
.empty { color:#666; }
</style>