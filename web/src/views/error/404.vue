<template>
  <div class="notfound">
    <div class="notfound-code kob-num">404</div>
    <div class="notfound-text">页面走丢了，这条蛇可能一头撞进了墙里</div>
    <a-button type="primary" size="large" class="notfound-btn" @click="goHome">
      返回首页
    </a-button>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";

const router = useRouter();
const goHome = () => router.push("/pk");
</script>

<style lang="scss" scoped>
.notfound {
  min-height: 62vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 40px 16px;
  text-align: center;
}

.notfound-code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 4px;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 18px 40px rgba(0, 0, 0, 0.45);
}

.notfound-text {
  color: var(--kob-text-dim);
  font-size: 1rem;
  margin-bottom: 14px;
}

.notfound-btn {
  min-width: 140px;
  border-radius: 999px;
  font-weight: 600;
}
</style>
