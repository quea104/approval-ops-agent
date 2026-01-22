<script setup>
import { computed } from "vue";
import { getUsername } from "./services/auth";

const username = computed(() => getUsername());
</script>

<template>
  <div class="wrap">
    <header class="top">
      <div class="brand" @click="$router.push('/requests')">
        승인 기반 업무 계획 에이전트
      </div>

      <nav class="nav">
        <RouterLink class="link" to="/requests" :class="{ active: $route.path.startsWith('/requests') }">
          요청
        </RouterLink>
        <span class="sep"></span>
        <span class="meta">사용자: {{ username }}</span>
      </nav>
    </header>

    <main class="main">
      <RouterView />
    </main>

    <footer class="foot">
      <span>흐름: 요청 → 계획(에이전트) → 승인 → 실행(도구) → 감사로그</span>
    </footer>
  </div>
</template>

<style scoped>
.wrap { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; color: #111; }
.top { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-bottom:1px solid #e6e6e6; }
.brand { font-weight:900; cursor:pointer; }
.nav { display:flex; align-items:center; gap:10px; }
.link { text-decoration:none; color:#333; padding:6px 10px; border-radius:10px; }
.link.active { background:#f3f3f3; }
.sep { width:1px; height:18px; background:#ddd; margin:0 6px; }
.meta { font-size:13px; color:#666; }
.main { padding:18px; max-width:1020px; margin:0 auto; }
.foot { padding:14px 18px; border-top:1px solid #eee; color:#777; font-size:12px; }
</style>
