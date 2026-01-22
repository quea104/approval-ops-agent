import { createRouter, createWebHistory } from "vue-router";
import Login from "./components/Login.vue";
import Requests from "./components/Requests.vue";
import RequestDetail from "./components/RequestDetail.vue";
import { getToken } from "./services/auth";

const routes = [
    { path: "/", redirect: "/requests"},
    { path: "/login", component: Login },
    { path: "/requests", component: Requests, meta: {requiredAuth: false } },
    { path: "/requests/:id", component: RequestDetail, meta: {requiredAuth: false } },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach((to) => {
    if (to.meta.requiresAuth && !getToken()) return "/login";
});

export default router;