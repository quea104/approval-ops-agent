<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { setAuth } from "../services/auth.js";

const router = useRouter();
const username = ref("admin");
const password = ref("");

function fakeLogin() {
  setAuth({ username: username.value, token: "" });
  router.push("/requests");
}
</script>

<template>
  <div class="card">
    <h2>로그인(임시)</h2>
    <p class="sub">로그인 API 개발 후 연결</p>

    <div class="row">
      <label>사용자 이름</label>
      <input v-model="username" placeholder="admin" />
    </div>

    <div class="row">
      <label>비밀번호</label>
      <input v-model="password" type="password" placeholder="***" />
    </div>

    <button class="primary" @click="fakeLogin">임시 로그인</button>
    <p class="hint">무조건 인증 통과</p>
  </div>
</template>

<style scoped>
.card { max-width:480px; margin:60px auto; border:1px solid #e6e6e6; border-radius:14px; padding:18px; }
h2 { margin:0 0 6px; }
.sub { margin:0 0 12px; color:#666; font-size:13px; }
.row { display:grid; gap:6px; margin:12px 0; }
label { font-size:13px; color:#444; }
input { padding:10px 12px; border:1px solid #ddd; border-radius:10px; outline:none; }
.primary { width:100%; margin-top:10px; padding:10px 12px; border:0; border-radius:12px; cursor:pointer; background:#111; color:#fff; }
.hint { margin-top:12px; color:#777; font-size:12px; }
</style>