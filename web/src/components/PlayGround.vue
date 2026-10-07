<template>
  <div class="playground">
    <div class="arena-frame">
      <game-map />
    </div>
  </div>
</template>

<script setup>
import GameMap from "./GameMap.vue";
</script>

<style lang="scss" scoped>
.playground {
  width: min(720px, 92vw);
  margin: 26px auto 32px;
}

.arena-frame {
  background: linear-gradient(180deg, #17241f, #101a16);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--kob-radius-lg);
  box-shadow: var(--kob-shadow);
  padding: clamp(8px, 1.6vw, 14px);
}
</style>
