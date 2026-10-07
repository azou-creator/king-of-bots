<template>
  <nav class="nav">
    <Link
      v-for="item in mainLinks"
      :key="item.path"
      class="nav-link"
      :class="{ active: isActive(item.path) }"
      :to="item.path"
    >
      {{ item.label }}
    </Link>

    <div class="nav-spacer"></div>

    <template v-if="$store.state.user.is_login">
      <a-dropdown placement="bottomRight">
        <div class="nav-user">
          <a-avatar :src="$store.state.user.avatar" :size="30" alt="头像" />
          <span class="nav-username">{{ $store.state.user.username }}</span>
          <svg class="nav-caret" viewBox="0 0 12 12" aria-hidden="true">
            <path d="M2.5 4.5 6 8l3.5-3.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <template #overlay>
          <a-menu>
            <a-menu-item key="bots">
              <Link class="dropdown-item" to="/bot">我的 Bots</Link>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout" @click="logout">退出登录</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </template>
    <template v-else>
      <Link class="nav-link" :class="{ active: isActive('/login') }" to="/login">
        登录
      </Link>
      <Link to="/register">
        <a-button size="small" type="primary" class="nav-register">注册</a-button>
      </Link>
    </template>
  </nav>
</template>

<script setup>
import Link from "@/components/Link.vue";
import { useStore } from "vuex";
import { useRoute } from "vue-router";

const store = useStore();
const route = useRoute();

const mainLinks = [
  { path: "/pk", label: "对战" },
  { path: "/record", label: "对局列表" },
  { path: "/rankList", label: "排行榜" },
];

// 对战回放页(/videotape/...)在导航上仍归属「对局列表」
const isActive = (path) => {
  if (path === "/record") {
    return route.path.startsWith("/record") || route.path.startsWith("/videotape");
  }
  return route.path.startsWith(path);
};

const logout = () => {
  store.dispatch("logout");
};
</script>

<style lang="scss" scoped>
.nav {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.nav-link {
  color: rgba(255, 255, 255, 0.72);
  text-decoration: none;
  font-size: 0.95rem;
  padding: 6px 14px;
  border-radius: 999px;
  transition: color 0.2s ease, background-color 0.2s ease;
  white-space: nowrap;

  &:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.09);
  }

  &.active {
    color: #fff;
    background: var(--kob-accent);
  }
}

.nav-spacer {
  flex: 1;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 12px 5px 6px;
  border-radius: 999px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.88);
  transition: background-color 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.09);
  }
}

.nav-username {
  font-size: 0.92rem;
  max-width: 9em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-caret {
  width: 11px;
  height: 11px;
  opacity: 0.65;
}

.nav-register {
  border-radius: 999px;
  padding: 0 15px;
}

.dropdown-item {
  color: inherit;
  text-decoration: none;
}

@media (max-width: 560px) {
  .nav {
    gap: 2px;
  }
  .nav-link {
    padding: 6px 8px;
    font-size: 0.88rem;
  }
  .nav-username,
  .nav-caret {
    display: none;
  }
  .nav-user {
    padding: 4px;
  }
  .nav-register {
    padding: 0 10px;
  }
}
</style>
