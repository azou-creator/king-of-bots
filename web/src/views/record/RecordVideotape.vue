<template>
  <div class="replay-page">
    <div class="replay-bar">
      <a-button size="small" class="back-btn" @click="goBack">
        <svg class="back-icon" viewBox="0 0 12 12" aria-hidden="true">
          <path d="M7.5 2.5 4 6l3.5 3.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        返回对局列表
      </a-button>
      <span class="replay-title">对局回放</span>
    </div>
    <play-ground />
  </div>
</template>

<script setup>
import PlayGround from "@/components/PlayGround.vue";
import { useRouter } from "vue-router";

const router = useRouter();
const goBack = () => router.push("/record");
</script>

<style lang="scss" scoped>
.replay-bar {
  width: min(720px, 92vw);
  margin: 20px auto 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border-radius: 999px;
}

.back-icon {
  width: 11px;
  height: 11px;
}

.replay-title {
  color: rgba(255, 255, 255, 0.92);
  font-weight: 600;
  font-size: 0.95rem;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}
</style>
