<template>
  <a-config-provider :locale="zhCN">
    <div class="app">
      <!-- 固定背景层:背景图 + 压暗渐变,保证内容对比度且不随页面滚动重绘 -->
      <div class="app-bg" aria-hidden="true"></div>

      <header class="app-header">
        <div class="app-header-inner">
          <Link class="logo" to="/pk">
            <svg class="logo-mark" viewBox="0 0 32 32" fill="none" aria-hidden="true">
              <path
                d="M7 23c0-5.5 4.5-5.5 9-5.5s9 0 9-5.5-4.5-5.5-9-5.5"
                stroke="currentColor"
                stroke-width="3.2"
                stroke-linecap="round"
              />
              <circle cx="7.6" cy="23" r="2.8" fill="currentColor" />
            </svg>
            <span class="logo-text">King Of Bots</span>
          </Link>
          <NavBar />
        </div>
      </header>

      <main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <footer class="app-footer">King Of Bots · 贪吃蛇对战平台</footer>
    </div>
  </a-config-provider>
</template>

<script setup>
import NavBar from "@/components/NavBar.vue";
import zhCN from "ant-design-vue/es/locale/zh_CN";
</script>

<style lang="scss" scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

.app-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: url(@/assets/background.jpg) center / cover no-repeat;

  &::after {
    content: "";
    position: absolute;
    inset: 0;
    background: linear-gradient(
      180deg,
      rgba(9, 20, 15, 0.4) 0%,
      rgba(9, 20, 15, 0.58) 100%
    );
  }
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--kob-glass-dark);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.app-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 26px;
}

@media (max-width: 560px) {
  .app-header-inner {
    padding: 0 14px;
    gap: 12px;
  }

  /* 小屏下只保留蛇形图标,防止头部溢出 */
  .logo .logo-text {
    display: none;
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #fff;
  flex-shrink: 0;

  .logo-mark {
    width: 27px;
    height: 27px;
    color: var(--kob-accent);
    transition: transform 0.3s ease;
  }

  &:hover .logo-mark {
    transform: rotate(-8deg) scale(1.06);
  }

  .logo-text {
    font-size: 1.12rem;
    font-weight: 700;
    letter-spacing: 0.2px;
    white-space: nowrap;
  }
}

.app-main {
  flex: 1;
  position: relative;
  z-index: 1;
  width: 100%;
}

.app-footer {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 18px 16px 22px;
  color: var(--kob-text-dim);
  font-size: 0.85rem;
}
</style>
